/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.red.viewers;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.robotframework.ide.eclipse.main.plugin.RedImages;
import org.robotframework.red.graphics.ColorsManager;
import org.robotframework.red.graphics.ImagesManager;
import org.robotframework.red.jface.viewers.Stylers;

/**
 * Those objects are used in order to have additional entry in table which can
 * be handled by editing supports
 *
 * @author anglart
 */
public class ElementAddingToken {

    private final String newElementTypeName;
    private final boolean enabled;
    private final int rank;

    public ElementAddingToken(final String newElementTypeName, final boolean isEnabled) {
        this(newElementTypeName, isEnabled, 0);
    }

    public ElementAddingToken(final String newElementTypeName, final boolean isEnabled, final int rank) {
        this.newElementTypeName = newElementTypeName;
        this.enabled = isEnabled;
        this.rank = rank;
    }

    public Image getImage() {
        if (rank > 0) {
            return null;
        }
        final ImageDescriptor addImage = RedImages.getAddImage();
        return ImagesManager.getImage(enabled ? addImage : RedImages.getGrayedImage(addImage));
    }

    public StyledString getStyledText() {
        final String msg = rank == 0 ? "...add new" + " " + newElementTypeName : "...";
        final Color color = enabled ? ColorsManager.getColor(30, 127, 60) : ColorsManager.getColor(200, 200, 200);
        final int style = rank == 0 ? SWT.ITALIC : SWT.ITALIC | SWT.BOLD;
        return new StyledString(msg, Stylers.mixingStyler(Stylers.withForeground(color), Stylers.withFontStyle(style)));
    }
}
