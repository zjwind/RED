package org.robotframework.ide.eclipse.main.plugin.launch;

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class RobotLaunchConfiguration {

    static final String TYPE_ID = "org.robotframework.ide.robotLaunchConfiguration";

    private static final String EXECUTOR_ARGUMENTS_ATTRIBUTE = "Executor arguments";
    private static final String PROJECT_NAME_ATTRIBUTE = "Project name";
    private static final String TEST_SUITES_ATTRIBUTE = "Test suites";

    private final ILaunchConfiguration configuration;
    
    static ILaunchConfigurationWorkingCopy createDefault(final ILaunchConfigurationType launchConfigurationType,
            final List<IResource> resources) throws CoreException {

        final String name = resources.size() == 1 ? resources.get(0).getName() : resources.get(0).getProject()
                .getName();
        final String configurationName = DebugPlugin.getDefault().getLaunchManager()
                .generateLaunchConfigurationName(name);
        final ILaunchConfigurationWorkingCopy configuration = launchConfigurationType.newInstance(null,
                configurationName);
        fillDefaults(configuration, resources);
        configuration.doSave();
        return configuration;
    }

    public static void fillDefaults(final ILaunchConfigurationWorkingCopy launchConfig) {
        final RobotLaunchConfiguration robotConfig = new RobotLaunchConfiguration(launchConfig);
        robotConfig.setProjectName("");
        robotConfig.setExecutorArguments("");
        robotConfig.setSuitePaths(new ArrayList<String>());
    }

    private static void fillDefaults(final ILaunchConfigurationWorkingCopy launchConfig, final List<IResource> resources) {
        final RobotLaunchConfiguration robotConfig = new RobotLaunchConfiguration(launchConfig);
        robotConfig.setProjectName(resources.get(0).getProject().getName());
        robotConfig.setExecutorArguments("");
        robotConfig.setSuitePaths(newArrayList(Lists.transform(resources, new Function<IResource, String>() {
            @Override
            public String apply(final IResource resource) {
                return resource.getProjectRelativePath().toPortableString();
            }
        })));
    }

    public RobotLaunchConfiguration(final ILaunchConfiguration config) {
        this.configuration = config;
    }

    private ILaunchConfigurationWorkingCopy asWorkingCopy() {
        return configuration instanceof ILaunchConfigurationWorkingCopy ? (ILaunchConfigurationWorkingCopy) configuration
                : null;
    }
    
    public void setProjectName(final String projectName) {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        if (launchCopy != null) {
            launchCopy.setAttribute(PROJECT_NAME_ATTRIBUTE, projectName);
        }
    }

    public void setExecutorArguments(final String arguments) {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        if (launchCopy != null) {
            launchCopy.setAttribute(EXECUTOR_ARGUMENTS_ATTRIBUTE, arguments);
        }
    }

    public void setSuitePaths(final List<String> names) {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        if (launchCopy != null) {
            launchCopy.setAttribute(TEST_SUITES_ATTRIBUTE, names);
        }
    }

    public String getProjectName() throws CoreException {
        return configuration.getAttribute(PROJECT_NAME_ATTRIBUTE, "");
    }

    public String getExecutorArguments() throws CoreException {
        return configuration.getAttribute(EXECUTOR_ARGUMENTS_ATTRIBUTE, "");
    }

    public List<String> getSuitePaths() throws CoreException {
        return configuration.getAttribute(TEST_SUITES_ATTRIBUTE, new ArrayList<String>());
    }

    public boolean isSuitableFor(final List<IResource> resources) {
        try {
            for (final IResource resource : resources) {
                final IProject project = resource.getProject();
                if (!getProjectName().equals(project.getName())) {
                    return false;
                }
                boolean exists = false;
                for (final String path : getSuitePaths()) {
                    final IResource res = project.findMember(Path.fromPortableString(path));
                    if (res != null && res.equals(resource)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    return false;
                }
            }
            return true;
        } catch (final CoreException e) {
            return false;
        }
    }
}
