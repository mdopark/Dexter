/**
 * Copyright (c) 2014 Samsung Electronics, Inc.,
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.samsung.sec.dexter.eclipse.ui;

import com.google.common.base.Strings;
import com.samsung.sec.dexter.core.config.DexterConfig;
import com.samsung.sec.dexter.core.config.IDexterStandaloneListener;
import com.samsung.sec.dexter.core.exception.DexterRuntimeException;
import com.samsung.sec.dexter.core.job.DexterJobFacade;
import com.samsung.sec.dexter.core.plugin.BaseDexterPluginManager;
import com.samsung.sec.dexter.core.plugin.IDexterPlugin;
import com.samsung.sec.dexter.core.plugin.IDexterPluginInitializer;
import com.samsung.sec.dexter.core.plugin.IDexterPluginManager;
import com.samsung.sec.dexter.core.util.DexterClient;
import com.samsung.sec.dexter.core.util.EmptyDexterClient;
import com.samsung.sec.dexter.core.util.IDexterClient;
import com.samsung.sec.dexter.core.util.IDexterLoginInfoListener;
import com.samsung.sec.dexter.eclipse.ui.login.LoginDialog;
import com.samsung.sec.dexter.eclipse.ui.login.Messages;
import com.samsung.sec.dexter.eclipse.ui.util.EclipseLog;
import com.samsung.sec.dexter.eclipse.ui.util.EclipseUtil;
import com.samsung.sec.dexter.executor.DexterExecutorActivator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class DexterUIActivator extends AbstractUIPlugin implements IDexterPluginInitializer, IDexterStandaloneListener {
    public static final String PLUGIN_ID = "dexter-eclipse-ui"; //$NON-NLS-1$
    public final static EclipseLog LOG = new EclipseLog(PLUGIN_ID);

    private static DexterUIActivator plugin;
    private IDexterClient client = new EmptyDexterClient();;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(6);

    private DexterJobFacade jobFacade;
    private ScheduledFuture<?> loginFuture = null;

    private IDexterPluginManager pluginManager;

    private final List<IDexterLoginInfoListener> loginInfoListenerList = new ArrayList<IDexterLoginInfoListener>();

    /**
     * The constructor
     */
    public DexterUIActivator() {}

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
     * BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        LOG.setPlugin(this);

        final boolean isStandalone = getPreferenceStore().getBoolean("isStandalone");
        final String dexterHome = getPreferenceStore().getString(DexterConfig.DEXTER_HOME_KEY);
        final String id = getPreferenceStore().getString("userId");
        final String pwd = getPreferenceStore().getString("userPwd");
        final String serverAddress = getPreferenceStore().getString("serverAddress");

        initDexter(dexterHome, isStandalone, id, pwd, serverAddress);
        client.login();
    }

    // TODO 아래 실행 순서 다시 확인하기
    /**
     * Initialize belows:
     * 1. DexterConfig
     * 2. DexterClient
     * 3. DexterPluginManager
     * 4. Jobs -
     * 
     * @param dexterHome
     * @param isStandalone
     * @param id
     * @param pwd
     * @param serverAddress
     */
    public void initDexter(final String dexterHome, final boolean isStandalone, final String id,
            final String pwd, final String serverAddress) {
        try {
            initDexterConfig(dexterHome, isStandalone);
            initDexterClient(isStandalone, id, pwd, serverAddress);

            pluginManager = new BaseDexterPluginManager(this, client);
            pluginManager.initDexterPlugins();

            if (isStandalone) {
                stopJobs();
            } else {
                startJobs();
            }
        } catch (DexterRuntimeException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void initDexterClient(final boolean isStandalone, final String id,
            final String pwd, final String serverAddress) {
        if (isStandalone) {
            client = new EmptyDexterClient();
            return;
        }

        if (Strings.isNullOrEmpty(id) || Strings.isNullOrEmpty(pwd) || Strings.isNullOrEmpty(serverAddress)) {
            LOG.info(
                    "Initialize failure for Connection of Dexter Server because no id, pwd, serverAddress. If you are using standalong mode Dexter, ignore this message");
            return;
        }

        // TODO:
        client = new DexterClient.DexterClientBuilder(id, pwd).dexterServerAddress(serverAddress).build();
    }

    private void initDexterConfig(final String dexterHome, final boolean isStandalone) {
        final DexterConfig config = DexterConfig.getInstance();
        config.setDexterHome(dexterHome);
        DexterConfig.getInstance().setStandalone(isStandalone);
        DexterConfig.getInstance().addDexterStandaloneListener(client);
        DexterConfig.getInstance().addDexterStandaloneListener(this);
    }

    private void startJobs() {
        startLoginScheduler();
        initDexterJobFacade();
        startMonitorForLogin();
    }

    private void stopJobs() {
        stopLoginScheduler();
        stopDexterJobFacade();
        scheduler.shutdown();
    }

    private void startLoginScheduler() {
        // TODO check the UI/non-UI Thread
        if (loginFuture == null || loginFuture.isDone()) {
            Runnable checkLoginJob = new Runnable() {
                @Override
                public void run() {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!DexterConfig.getInstance().getRunMode().equals(DexterConfig.RunMode.DAEMON))
                                loginJob(null);
                        }
                    });
                }
            };

            loginFuture = getDefault().scheduler.scheduleAtFixedRate(checkLoginJob, 5, DexterJobFacade.SLEEP_FOR_LOGIN,
                    TimeUnit.SECONDS);
        }
    }

    private void stopLoginScheduler() {
        if (loginFuture != null)
            loginFuture.cancel(false);
    }

    private void initDexterJobFacade() {
        if (jobFacade != null) {
            DexterConfig.getInstance().removeDexterStandaloneListener(jobFacade);
            jobFacade.shutdownScheduleService();
        }

        jobFacade = new DexterJobFacade(client);
        jobFacade.startDexterServerJobs();
        jobFacade.startGeneralJobs();
        DexterConfig.getInstance().addDexterStandaloneListener(jobFacade);
    }

    private void stopDexterJobFacade() {
        if (jobFacade != null)
            jobFacade.shutdownScheduleService();
    }

    private void startMonitorForLogin() {
        Runnable checkLoginJob = new Runnable() {
            @Override
            public void run() {
                loginJob(EclipseUtil.getActiveWorkbenchWindowShell());
            }
        };

        scheduler.scheduleAtFixedRate(checkLoginJob, 1, DexterJobFacade.SLEEP_FOR_LOGIN, TimeUnit.SECONDS);
    }

    private void loginJob(final Shell shell) {
        final Shell localShell;

        if (shell == null || shell.isDisposed()) {
            if (Display.getDefault() != null && Display.getDefault().getActiveShell() != null) {
                localShell = Display.getDefault().getActiveShell();
            } else {
                return;
            }
        } else {
            localShell = shell;
        }

        localShell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                if (Strings.isNullOrEmpty(DexterConfig.getInstance().getDexterHome()) == false
                        && Strings.isNullOrEmpty(client.getCurrentUserId()) == false) {
                    return;
                }

                final LoginDialog dialog = new LoginDialog(localShell);
                final int ret = dialog.open();

                if (ret == InputDialog.CANCEL) {
                    MessageDialog.openError(localShell, "Dexter Login Error", //$NON-NLS-1$
                            Messages.LoginDialog_LOGIN_GUIDE_MSG);
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
     * BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;

        DexterConfig.getInstance().removeDexterStandaloneListener(client);
        DexterConfig.getInstance().removeDexterStandaloneListener(this);
        DexterConfig.getInstance().removeDexterStandaloneListener(jobFacade);
        stopDexterJobFacade();
        super.stop(context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.samsung.sec.dexter.executor.DexterPluginInitializer#init(java.util.
     * List)
     */
    @Override
    public void init(List<IDexterPlugin> pluginList) {
        IExtension[] extensions = Platform.getExtensionRegistry()
                .getExtensionPoint(DexterExecutorActivator.PLUGIN_ID, "DexterPlugin").getExtensions();

        if (extensions.length == 0) {
            throw new DexterRuntimeException("There is no Extensions for Static Analysis Eclipse Plug-ins");
        }

        for (int i = 0; i < extensions.length; i++) {
            final IConfigurationElement[] configs = extensions[i].getConfigurationElements();

            if (configs.length == 0) {
                DexterUIActivator.LOG.warn("cannot load Dexter Plugin : " + extensions[i].getLabel());
                continue;
            }

            initDexterPlugin(pluginList, configs);
        }

        if (pluginList.size() == 0) {
            throw new DexterRuntimeException("There are no dexter plug-ins to add");
        }
    }

    private void initDexterPlugin(List<IDexterPlugin> pluginHandlerList, final IConfigurationElement[] configs) {
        for (final IConfigurationElement config : configs) {
            IDexterPlugin plugin = null;
            try {
                Object o = config.createExecutableExtension("class");

                if (o instanceof IDexterPlugin) {
                    plugin = (IDexterPlugin) o;
                } else {
                    continue;
                }

                LOG.info(config.getAttribute("class") + " has been loaded");

                if (!pluginHandlerList.contains(plugin)) {
                    plugin.init();
                    pluginHandlerList.add(plugin);
                }
            } catch (Exception e) {
                // even one of plug-in has problem, Dexter should be able to
                // run.
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static DexterUIActivator getDefault() {
        return plugin;
    }

    public IDexterPluginManager getPluginManager() {
        return this.pluginManager;
    }

    @Override
    public void handleWhenStandaloneMode() {
        stopLoginScheduler();
    }

    @Override
    public void handleWhenNotStandaloneMode() {
        startLoginScheduler();
    }

    public synchronized void addLoginInfoListener(final IDexterLoginInfoListener listener) {
        if (!loginInfoListenerList.contains(listener)) {
            loginInfoListenerList.add(listener);
        }
    }

    public synchronized void removeLoginInfoListener(final IDexterLoginInfoListener listener) {
        loginInfoListenerList.remove(listener);
    }

    public synchronized void runLoginInfoHandler() {
        for (int i = 0; i < loginInfoListenerList.size(); i++) {
            final IDexterLoginInfoListener listener = loginInfoListenerList.get(i);

            if (listener != null) {
                listener.handleDexterLoginInfoChanged();
            } else {
                loginInfoListenerList.remove(i--);
            }
        }
    }

    public IDexterClient getDexterClient() {
        return this.client;
    }

    public void setDexterPreferences(final String serverAddress, final String id, final String pwd,
            final boolean isStandalone,
            final String dexterHomePath) {
        final IPreferenceStore store = DexterUIActivator.getDefault().getPreferenceStore();

        store.setValue("userId", id); //$NON-NLS-1$
        store.setValue("userPwd", pwd); //$NON-NLS-1$
        store.setValue("serverAddress", serverAddress); //$NON-NLS-1$
        store.setValue("isStandalone", isStandalone);
        store.setValue(DexterConfig.DEXTER_HOME_KEY, dexterHomePath);
        System.setProperty(DexterConfig.DEXTER_HOME_KEY, dexterHomePath);
    }
}
