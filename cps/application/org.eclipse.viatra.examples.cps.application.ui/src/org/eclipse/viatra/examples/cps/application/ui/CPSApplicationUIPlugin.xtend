/*******************************************************************************
 * Copyright (c) 2010-2017, Marton Elekes, IncQuery Labs Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Marton Elekes - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.examples.cps.application.ui

import org.eclipse.jface.resource.ImageRegistry
import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.BundleContext

/**
 * The activator class controls the plug-in life cycle
 * @author Marton Elekes
 */
class CPSApplicationUIPlugin extends AbstractUIPlugin {

    private static CPSApplicationUIPlugin plugin

    public static val ICON_LOAD_QUERY = "load_query"
    public static val ICON_RUN = "run"
    public static val ICON_STOP = "stop"

    public override void start(BundleContext context) throws Exception {
        super.start(context)
        plugin = this
    }

    public override void stop(BundleContext context) throws Exception {
        plugin = null
        super.stop(context)
    }

    public def static CPSApplicationUIPlugin getDefault() {
        return plugin
    }

    protected override void initializeImageRegistry(ImageRegistry reg) {
        super.initializeImageRegistry(reg)
        reg => [
            put(ICON_LOAD_QUERY,
                imageDescriptorFromPlugin("org.eclipse.viatra.query.tooling.ui.browser", "/icons/load_query.png"))
            put(ICON_RUN, imageDescriptorFromPlugin("org.eclipse.viatra.query.tooling.ui", "/icons/load.gif"))
            put(ICON_STOP, imageDescriptorFromPlugin("org.eclipse.viatra.query.tooling.ui", "/icons/unload.gif"))
        ]
    }
}
