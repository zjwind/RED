/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.model.cmd.variables;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.rf.ide.core.testdata.model.presenter.update.VariableTableModelUpdater;
import org.rf.ide.core.testdata.model.table.variables.AVariable.VariableType;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModelEvents;
import org.robotframework.ide.eclipse.main.plugin.model.RobotVariable;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.EditorCommand;

public class SetScalarValueCommand extends EditorCommand {

    private final RobotVariable variable;

    private final String newValue;
    
    private String previousValue;

    public SetScalarValueCommand(final RobotVariable variable, final String newValue) {
        this.variable = variable;
        this.newValue = newValue == null ? "" : newValue;
    }

    @Override
    public void execute() throws CommandExecutionException {
        previousValue = variable.getValue();
        if (variable.getType() != VariableType.SCALAR) {
            throw new CommandExecutionException("Invalid type of variable: " + variable.getType());
        }

        new VariableTableModelUpdater().addOrSet(variable.getLinkedElement(), 0, newArrayList(newValue));

        eventBroker.send(RobotModelEvents.ROBOT_VARIABLE_VALUE_CHANGE, variable);
    }
    
    @Override
    public List<EditorCommand> getUndoCommands() {
        return newUndoCommands(new SetScalarValueCommand(variable, previousValue));
    }
}
