/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.function.Predicate;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.rf.ide.core.project.RobotProjectConfig;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;

public class RobotProjectNature implements IProjectNature {

    public static final String ROBOT_NATURE = RedPlugin.PLUGIN_ID + ".robotNature";

    private static final String ROBOT_LIBRARIES_BUILDER = RedPlugin.PLUGIN_ID + ".robotLibrariesBuilder";

    private IProject project;

    public static void addRobotNature(final IProject project, final IProgressMonitor monitor,
            final Predicate<String> shouldReplaceConfig) throws CoreException {
        final IProjectDescription desc = project.getDescription();

        final List<String> natures = newArrayList(desc.getNatureIds());
        natures.add(ROBOT_NATURE);
        desc.setNatureIds(natures.toArray(new String[0]));

        project.setDescription(desc, monitor);

        final IFile cfgFile = project.getFile(RobotProjectConfig.FILENAME);
        if (!cfgFile.exists() || shouldReplaceConfig.test(project.getName())) {
            new RedEclipseProjectConfigWriter().writeConfiguration(RobotProjectConfig.create(), project);
        }
    }

    public static void removeRobotNature(final IProject project, final IProgressMonitor monitor,
            final Predicate<String> shouldRemoveConfig) throws CoreException {
        final IProjectDescription desc = project.getDescription();

        final List<String> natures = newArrayList(desc.getNatureIds());
        natures.remove(ROBOT_NATURE);
        desc.setNatureIds(natures.toArray(new String[0]));

        project.setDescription(desc, monitor);

        final IFile cfgFile = project.getFile(RobotProjectConfig.FILENAME);
        if (cfgFile.exists() && shouldRemoveConfig.test(project.getName())) {
            cfgFile.delete(true, null);
        }
    }

    public static boolean hasRobotNature(final IProject project) {
        try {
            return project.hasNature(ROBOT_NATURE);
        } catch (final CoreException e) {
            return false;
        }
    }

    @Override
    public void configure() throws CoreException {
        addToBuildSpec(project, ROBOT_LIBRARIES_BUILDER);
    }

    @Override
    public void deconfigure() throws CoreException {
        removeFromBuildSpec(project, ROBOT_LIBRARIES_BUILDER);
    }

    @Override
    public IProject getProject() {
        return project;
    }

    @Override
    public void setProject(final IProject project) {
        this.project = project;
    }

    private void addToBuildSpec(final IProject project, final String builderId) throws CoreException {
        if (project == null) {
            return;
        }
        final IProjectDescription description = project.getDescription();
        final ICommand[] commands = description.getBuildSpec();
        for (final ICommand command : commands) {
            if (command.getBuilderName().equals(builderId)) {
                return; // builder is already installed
            }
        }
        final ICommand command = description.newCommand();
        command.setBuilderName(builderId);
        final ICommand[] newCommands = new ICommand[commands.length + 1];
        System.arraycopy(commands, 0, newCommands, 0, commands.length);
        newCommands[newCommands.length - 1] = command;
        description.setBuildSpec(newCommands);
        project.setDescription(description, null);
    }

    private void removeFromBuildSpec(final IProject project, final String builderId) throws CoreException {
        if (project == null) {
            return;
        }

        final IProjectDescription description = project.getDescription();
        final ICommand[] commands = description.getBuildSpec();
        for (int i = 0; i < commands.length; ++i) {
            if (commands[i].getBuilderName().equals(builderId)) {
                final ICommand[] newCommands = new ICommand[commands.length - 1];
                System.arraycopy(commands, 0, newCommands, 0, i);
                System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
                description.setBuildSpec(newCommands);
                project.setDescription(description, null);
                return;
            }
        }
    }
}
