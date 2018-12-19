/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.launch.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewersConfigurator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.robotframework.ide.eclipse.main.plugin.RedImages;
import org.robotframework.ide.eclipse.main.plugin.model.LibspecsFolder;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCasesSection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCodeHoldingElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModelManager;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.model.RobotTasksSection;
import org.robotframework.ide.eclipse.main.plugin.project.ASuiteFileDescriber;
import org.robotframework.red.graphics.ImagesManager;
import org.robotframework.red.viewers.RedCommonLabelProvider;
import org.robotframework.red.viewers.Selections;
import org.robotframework.red.viewers.TreeContentProvider;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author mmarzec
 */
class SuitesToRunComposite extends Composite {

    private String projectName;

    private CheckboxTreeViewer viewer;

    private final Map<EButton, Button> buttons = new HashMap<>();

    private final List<SuiteLaunchElement> suitesToLaunch = new ArrayList<>();

    private final SuitesListener listener;

    SuitesToRunComposite(final Composite parent, final SuitesListener listener) {
        super(parent, SWT.NONE);
        this.listener = listener;

        GridLayoutFactory.fillDefaults().numColumns(2).margins(2, 1).applyTo(this);
        createViewer();
        createAllButtons();
    }

    private void createViewer() {
        viewer = new CheckboxTreeViewer(this, SWT.MULTI | SWT.BORDER | SWT.CHECK);
        viewer.setUseHashlookup(true);
        ViewersConfigurator.enableDeselectionPossibility(viewer);
        GridDataFactory.fillDefaults().grab(true, true).span(1, 5).minSize(200, 130).applyTo(viewer.getTree());

        viewer.setCheckStateProvider(new CheckStateProvider());
        viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new CheckboxTreeViewerLabelProvider()));
        viewer.setContentProvider(new CheckboxTreeViewerContentProvider());
        viewer.setComparator(new ViewerComparator() {

            @Override
            public int category(final Object element) {
                return ((SuiteLaunchElement) element).resource.getType() == IResource.FOLDER ? 0 : 1;
            }

            @Override
            public int compare(final Viewer viewer, final Object e1, final Object e2) {
                final int cat1 = category(e1);
                final int cat2 = category(e2);

                if (cat1 != cat2) {
                    return cat1 - cat2;
                }

                final IResource resource1 = ((SuiteLaunchElement) e1).resource;
                final IResource resource2 = ((SuiteLaunchElement) e2).resource;

                final String path1 = resource1.getFullPath().toString();
                final String path2 = resource2.getFullPath().toString();

                return path1.compareToIgnoreCase(path2);
            }
        });
        viewer.addCheckStateListener(event -> {
            final Object element = event.getElement();
            final boolean isElementChecked = event.getChecked();

            if (element instanceof SuiteLaunchElement) {
                ((SuiteLaunchElement) element).updateChecked(isElementChecked);
            } else if (element instanceof TestCaseLaunchElement) {
                ((TestCaseLaunchElement) element).updateChecked(isElementChecked);
            }
            listener.suitesChanged();
        });
        viewer.addSelectionChangedListener(event -> {
            final Button removeButton = buttons.get(EButton.REMOVE);
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (selection.isEmpty()) {
                removeButton.setEnabled(false);
                return;
            }
            final List<SuiteLaunchElement> suites = Selections.getElements(selection, SuiteLaunchElement.class);
            final List<TestCaseLaunchElement> tests = Selections.getElements(selection, TestCaseLaunchElement.class);
            if (!suites.isEmpty() && tests.isEmpty()) {
                removeButton.setEnabled(true);
            } else if (suites.isEmpty() && !tests.isEmpty()) {
                boolean allAreMissing = true;
                for (final TestCaseLaunchElement test : tests) {
                    if (!test.isMissing) {
                        allAreMissing = false;
                        break;
                    }
                }
                removeButton.setEnabled(allAreMissing);
            } else {
                removeButton.setEnabled(false);
            }
        });
    }

    private void createAllButtons() {
        buttons.put(EButton.BROWSE, createBrowseButton());
        buttons.put(EButton.REMOVE, createRemoveButton());
        buttons.put(EButton.SELECT_ALL, createSelectButton());
        buttons.put(EButton.DESELECT_ALL, createDeselectButton());
    }

    private Button createBrowseButton() {
        final Button browseSuitesButton = new Button(this, SWT.PUSH);
        browseSuitesButton.setText("Browse...");
        GridDataFactory.fillDefaults().applyTo(browseSuitesButton);
        browseSuitesButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                final ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
                        new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
                dialog.setAllowMultiple(true);
                dialog.setTitle("Select suite");
                dialog.setMessage("Select suite to execute:");
                dialog.setComparator(new ViewerComparator() {

                    @Override
                    public int category(final Object element) {
                        return ((IResource) element).getType() == IResource.FOLDER ? 0 : 1;
                    }
                });
                dialog.addFilter(new ViewerFilter() {

                    @Override
                    public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
                        return element instanceof IResource && shouldShow((IResource) element);
                    }

                    private boolean shouldShow(final IResource resource) {
                        if (!resource.getProject().getName().equals(projectName)) {
                            return false;
                        } else if (resource.getType() == IResource.PROJECT) {
                            return true;
                        } else if (resource.getType() == IResource.FOLDER) {
                            return !resource.equals(LibspecsFolder.get(resource.getProject()).getResource());
                        } else if (resource.getType() == IResource.FILE
                                && (ASuiteFileDescriber.isSuiteFile((IFile) resource)
                                        || ASuiteFileDescriber.isRpaSuiteFile((IFile) resource))) {
                            final RobotSuiteFile suiteModel = RobotModelManager.getInstance()
                                    .createSuiteFile((IFile) resource);
                            return suiteModel.isSuiteFile() || suiteModel.isRpaSuiteFile();
                        }
                        return false;
                    }
                });
                dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
                if (dialog.open() == Window.OK) {
                    for (final Object obj : dialog.getResult()) {
                        final IResource chosenResource = (IResource) obj;
                        if (chosenResource.getType() == IResource.PROJECT) {
                            continue;
                        }
                        final SuiteLaunchElement suite = new SuiteLaunchElement(chosenResource);
                        for (final String caseName : getCaseNames(chosenResource)) {
                            suite.addChild(new TestCaseLaunchElement(suite, caseName, true, false));
                        }
                        if (!suitesToLaunch.contains(suite)) {
                            suitesToLaunch.add(suite);
                        }
                    }
                    listener.suitesChanged();
                }
            }
        });
        return browseSuitesButton;
    }

    private Button createRemoveButton() {
        final Button removeSuite = new Button(this, SWT.PUSH);
        GridDataFactory.fillDefaults().applyTo(removeSuite);
        removeSuite.setText("Remove");
        removeSuite.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                final IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                final List<SuiteLaunchElement> suites = Selections.getElements(selection, SuiteLaunchElement.class);

                boolean changed = false;
                if (!suites.isEmpty()) {
                    suitesToLaunch.removeAll(suites);
                    changed = true;
                }
                final List<TestCaseLaunchElement> tests = Selections.getElements(selection,
                        TestCaseLaunchElement.class);
                for (final TestCaseLaunchElement test : tests) {
                    test.getParent().getChildren().remove(test);
                    changed = true;
                }
                if (changed) {
                    listener.suitesChanged();
                }
            }
        });
        return removeSuite;
    }

    private Button createSelectButton() {
        final Button selectAll = new Button(this, SWT.PUSH);
        GridDataFactory.fillDefaults().indent(0, 10).applyTo(selectAll);
        selectAll.setText("Select All");
        selectAll.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                setLaunchElementsChecked(true);
                listener.suitesChanged();
            }
        });
        return selectAll;
    }

    private Button createDeselectButton() {
        final Button deselectAll = new Button(this, SWT.PUSH);
        GridDataFactory.fillDefaults().applyTo(deselectAll);
        deselectAll.setText("Deselect All");
        deselectAll.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                setLaunchElementsChecked(false);
                listener.suitesChanged();
            }
        });
        return deselectAll;
    }

    @Override
    public void dispose() {
        buttons.clear();
        super.dispose();
    }

    void switchTo(final String projectName) {
        this.projectName = projectName;

        for (final SuiteLaunchElement suite : suitesToLaunch) {
            if (!projectName.isEmpty()) {
                suite.updateProject(projectName);
            }
        }
        viewer.refresh();
    }

    private void setLaunchElementsChecked(final boolean isChecked) {
        final List<TestCaseLaunchElement> testsToCheck = new ArrayList<>();
        final List<SuiteLaunchElement> suitesToCheck = new ArrayList<>();

        if (viewer.getTree().getSelectionCount() == 0) {
            suitesToCheck.addAll(suitesToLaunch);
        } else {
            suitesToCheck
                    .addAll(Selections.getElements((TreeSelection) viewer.getSelection(), SuiteLaunchElement.class));
            testsToCheck
                    .addAll(Selections.getElements((TreeSelection) viewer.getSelection(), TestCaseLaunchElement.class));
        }
        for (final TestCaseLaunchElement test : testsToCheck) {
            test.updateChecked(isChecked);
        }
        for (final SuiteLaunchElement suite : suitesToCheck) {
            suite.updateChecked(isChecked);
        }
    }

    void setInput(final String projectName, final Map<String, List<String>> suitesToRun) {
        this.projectName = projectName;
        suitesToLaunch.clear();
        viewer.setInput(suitesToLaunch);
        final Object[] checked = viewer.getExpandedElements();
        try {
            viewer.getTree().setRedraw(false);

            if (projectName.isEmpty()) {
                return;
            }
            final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            suitesToRun.forEach((pathString, caseNames) -> {
                suitesToLaunch.add(extractSuite(project, Path.fromPortableString(pathString), caseNames));
            });
        } finally {
            viewer.refresh();
            viewer.setExpandedElements(checked);
            viewer.getTree().setRedraw(true);
        }
    }

    private SuiteLaunchElement extractSuite(final IProject project, final IPath path, final List<String> caseNames) {
        final IResource resource = path.getFileExtension() == null ? project.getFolder(path) : project.getFile(path);
        final SuiteLaunchElement suite = new SuiteLaunchElement(resource);

        final List<String> missingCaseNames = new ArrayList<>(caseNames);

        for (final String caseName : getCaseNames(resource)) {
            final boolean isChecked = caseNames.isEmpty() || caseNames.contains(caseName.toLowerCase());
            suite.addChild(new TestCaseLaunchElement(suite, caseName, isChecked, false));
            missingCaseNames.remove(caseName.toLowerCase());
        }
        for (final String missingCaseName : missingCaseNames) {
            suite.addChild(new TestCaseLaunchElement(suite, missingCaseName, true, true));
        }
        return suite;
    }

    private static List<String> getCaseNames(final IResource resource) {
        final List<RobotCodeHoldingElement<?>> cases = new ArrayList<>();

        if (resource.exists() && resource.getType() == IResource.FILE
                && (ASuiteFileDescriber.isSuiteFile((IFile) resource)
                        || ASuiteFileDescriber.isRpaSuiteFile((IFile) resource))) {
            final RobotSuiteFile suiteModel = RobotModelManager.getInstance().createSuiteFile((IFile) resource);
            if (suiteModel.isSuiteFile()) {
                final Optional<RobotCasesSection> section = suiteModel.findSection(RobotCasesSection.class);
                if (section.isPresent()) {
                    cases.addAll(section.get().getChildren());
                }

            } else if (suiteModel.isRpaSuiteFile()) {
                final Optional<RobotTasksSection> section = suiteModel.findSection(RobotTasksSection.class);
                if (section.isPresent()) {
                    cases.addAll(section.get().getChildren());
                }
            }
        }
        return cases.stream().map(RobotCodeHoldingElement::getName).collect(Collectors.toList());
    }

    Map<String, List<String>> extractSuitesToRun() {
        final Map<String, List<String>> suitesToRun = new LinkedHashMap<>();

        boolean allTestsSelected = true;
        for (final SuiteLaunchElement suite : suitesToLaunch) {
            if (suite.isChecked()) {
                allTestsSelected = allTestsSelected && suite.hasCheckedAllChildren();
                final List<String> tests = new ArrayList<>();
                for (final TestCaseLaunchElement test : suite.getChildren()) {
                    if (test.isChecked()) {
                        tests.add(test.getName().toLowerCase());
                    }
                }
                suitesToRun.put(suite.getPath(), tests);
            }
        }
        if (allTestsSelected) {
            suitesToRun.values().forEach(List::clear);
        }

        return suitesToRun;
    }

    @VisibleForTesting
    static class CheckboxTreeViewerContentProvider extends TreeContentProvider {

        @Override
        public Object[] getElements(final Object inputElement) {
            return ((List<?>) inputElement).toArray();
        }

        @Override
        public Object[] getChildren(final Object parentElement) {
            if (parentElement instanceof SuiteLaunchElement) {
                final List<TestCaseLaunchElement> children = ((SuiteLaunchElement) parentElement).getChildren();
                return children.toArray(new TestCaseLaunchElement[children.size()]);
            }
            return null;
        }

        @Override
        public Object getParent(final Object element) {
            if (element instanceof TestCaseLaunchElement) {
                return ((TestCaseLaunchElement) element).getParent();
            }
            return null;
        }

        @Override
        public boolean hasChildren(final Object element) {
            return element instanceof SuiteLaunchElement && !((SuiteLaunchElement) element).getChildren().isEmpty();
        }
    }

    @VisibleForTesting
    static class CheckboxTreeViewerLabelProvider extends RedCommonLabelProvider {

        @Override
        public StyledString getStyledText(final Object element) {
            if (element instanceof SuiteLaunchElement) {
                return new StyledString(((SuiteLaunchElement) element).getPath());
            } else {
                return new StyledString(((TestCaseLaunchElement) element).getName());
            }
        }

        @Override
        public Image getImage(final Object element) {
            if (element instanceof SuiteLaunchElement) {
                return ((SuiteLaunchElement) element).getImage();
            } else {
                return ((TestCaseLaunchElement) element).getImage();
            }
        }
    }

    @VisibleForTesting
    static class CheckStateProvider implements ICheckStateProvider {

        @Override
        public boolean isChecked(final Object element) {
            if (element instanceof SuiteLaunchElement) {
                return ((SuiteLaunchElement) element).isChecked();
            } else {
                return ((TestCaseLaunchElement) element).isChecked();
            }
        }

        @Override
        public boolean isGrayed(final Object element) {
            if (element instanceof SuiteLaunchElement) {
                final SuiteLaunchElement suite = (SuiteLaunchElement) element;
                return suite.hasCheckedChild() && !suite.hasCheckedAllChildren();
            }
            return false;
        }
    }

    @VisibleForTesting
    static final class SuiteLaunchElement {

        private IResource resource;

        private final List<TestCaseLaunchElement> children;

        private boolean isChecked = true;

        SuiteLaunchElement(final IResource resource) {
            this.resource = resource;
            this.children = new ArrayList<>();
        }

        Image getImage() {
            final ImageDescriptor baseImage = resource.getType() == IResource.FILE ? RedImages.getRobotFileImage()
                    : RedImages.getFolderImage();
            if (resource.exists()) {
                return ImagesManager.getImage(baseImage);
            } else {
                return ImagesManager.getImage(new DecorationOverlayIcon(ImagesManager.getImage(baseImage),
                        RedImages.getErrorImage(), IDecoration.BOTTOM_LEFT));
            }
        }

        String getPath() {
            return resource.getProjectRelativePath().toPortableString();
        }

        List<TestCaseLaunchElement> getChildren() {
            return children;
        }

        void addChild(final TestCaseLaunchElement child) {
            children.add(child);
        }

        boolean isChecked() {
            return isChecked;
        }

        void setChecked(final boolean isChecked) {
            this.isChecked = isChecked;
        }

        void updateChecked(final boolean isChecked) {
            this.isChecked = isChecked;
            for (final TestCaseLaunchElement testCaseElement : children) {
                testCaseElement.setChecked(isChecked);
            }
        }

        private boolean hasCheckedChild() {
            return children.stream().anyMatch(test -> test.isChecked());
        }

        private boolean hasCheckedAllChildren() {
            return children.stream().allMatch(test -> test.isChecked());
        }

        public void updateProject(final String projectName) {
            final IProject newProject = resource.getWorkspace().getRoot().getProject(projectName);
            final IPath path = resource.getProjectRelativePath();
            this.resource = path.getFileExtension() == null ? newProject.getFolder(path) : newProject.getFile(path);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            } else if (obj.getClass() == getClass()) {
                final SuiteLaunchElement other = (SuiteLaunchElement) obj;
                return Objects.equals(resource, other.resource);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(resource);
        }
    }

    @VisibleForTesting
    static final class TestCaseLaunchElement {

        private final String name;

        private final SuiteLaunchElement parent;

        private boolean isChecked;

        private final boolean isMissing;

        TestCaseLaunchElement(final SuiteLaunchElement parent, final String name, final boolean isChecked,
                final boolean isMissing) {
            this.name = name;
            this.parent = parent;
            this.isChecked = isChecked;
            this.isMissing = isMissing;
        }

        Image getImage() {
            final ImageDescriptor baseImage = RedImages.getTestCaseImage();
            if (isMissing) {
                return ImagesManager.getImage(new DecorationOverlayIcon(ImagesManager.getImage(baseImage),
                        RedImages.getErrorImage(), IDecoration.BOTTOM_LEFT));
            } else {
                return ImagesManager.getImage(baseImage);
            }
        }

        String getName() {
            return name;
        }

        SuiteLaunchElement getParent() {
            return parent;
        }

        boolean isChecked() {
            return isChecked;
        }

        void setChecked(final boolean isChecked) {
            this.isChecked = isChecked;
        }

        private void updateChecked(final boolean isChecked) {
            this.isChecked = isChecked;
            if (isChecked) {
                parent.setChecked(isChecked);
            } else if (!parent.hasCheckedChild()) {
                parent.setChecked(false);
            }
        }
    }

    private enum EButton {
        BROWSE,
        REMOVE,
        SELECT_ALL,
        DESELECT_ALL;
    }

    @FunctionalInterface
    public interface SuitesListener {

        void suitesChanged();
    }
}
