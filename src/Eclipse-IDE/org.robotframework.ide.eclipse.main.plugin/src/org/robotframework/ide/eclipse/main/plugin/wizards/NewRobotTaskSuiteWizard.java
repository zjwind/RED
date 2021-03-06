/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotFormEditor;

public class NewRobotTaskSuiteWizard extends BasicNewResourceWizard {

    private WizardNewRobotSuiteFileCreationPage mainPage;

    @Override
    public void init(final IWorkbench workbench, final IStructuredSelection currentSelection) {
        super.init(workbench, currentSelection);
        setNeedsProgressMonitor(true);
        setWindowTitle("New Robot Task Suite");
    }

    @Override
    public void addPages() {
        super.addPages();

        mainPage = new WizardNewRobotSuiteFileCreationPage("New Robot Task Suite", getSelection(), "*** Tasks ***");
        mainPage.setWizard(this);
        mainPage.setTitle("Robot Task Suite");
        mainPage.setDescription("Create new Robot task suite file");

        this.addPage(mainPage);
    }

    @Override
    public boolean performFinish() {
        mainPage.setExtension();
        final IFile newFile = mainPage.createNewFile();
        selectAndReveal(newFile);
        if (newFile.exists()) {
            RobotFormEditor.tryToOpen(newFile, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());
        }
        return true;
    }

}
