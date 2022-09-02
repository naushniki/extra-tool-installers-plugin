package io.jenkins.plugins.extratoolinstallers.installers;

import com.google.common.base.Joiner;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.extratoolinstallers.installers.utils.VersionChecker;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A {@link ToolInstaller} that tool is already installed in the specified directory and fails if it is not.
 */
public class IsAlreadyInGivenDirectory extends ToolInstaller {
    @CheckForNull
    private String executablePath;

    @CheckForNull
    private String relativePath;

    @CheckForNull
    private String[] versionCmd;

    @CheckForNull
    private Pattern versionPattern;

    /**
     * Only used if we've been given a pattern that can't be compiled
     */
    @CheckForNull
    private String versionPatternString;

    @CheckForNull
    private String versionMin;

    @CheckForNull
    private String versionMax;

    /**
     * Constructor that sets mandatory fields.
     *
     * @param label The {@link ToolInstaller#getLabel()}.
     */
    @DataBoundConstructor
    public IsAlreadyInGivenDirectory(String label) {
        super(label);
    }

    /**
     * Path to the executable file.
     *
     * @return Path, or null if none has been set.
     */
    @CheckForNull
    public String getExecutablePath() {
        return Util.fixEmpty(executablePath);
    }

    /**
     * Sets {@link #getExecutablePath()}.
     *
     * @param executablePath new value.
     */
    @DataBoundSetter
    public void setExecutablePath(@Nullable String executablePath) {
        this.executablePath = Util.fixEmpty(executablePath);
    }

    /**
     * Directory of the tool's "home", relative to executable path.
     *
     * @return The relative path, or null if none has been set (which means ".").
     */
    @CheckForNull
    public String getRelativePath() {
        return Util.fixEmpty(relativePath);
    }

    /**
     * Sets {@link #getRelativePath()}.
     *
     * @param relativePath New value.
     */
    @DataBoundSetter
    public void setRelativePath(@Nullable String relativePath) {
        this.relativePath = Util.fixEmpty(relativePath);
    }

    /**
     * Command we run in order to test what version we've got.
     *
     * @return null if not set, else a list where the first element is the command
     * and the remaining elements are arguments for the command.
     */
    @CheckForNull
    public String[] getVersionCmd() {
        return fixEmpty(versionCmd);
    }

    /**
     * See {@link #getVersionCmd()}.
     *
     * @return {@link #getVersionCmd()} as a multi-line string.
     */
    @Nonnull
    public String getVersionCmdString() {
        final String[] v = getVersionCmd();
        if (v == null) {
            return "";
        }
        return Joiner.on('\n').join(v);
    }

    /**
     * Sets {@link #getVersionCmd()}.
     *
     * @param versionCmd New value.
     */
    public void setVersionCmd(String[] versionCmd) {
        if (versionCmd != null) {
            this.versionCmd = Arrays.copyOf(versionCmd, versionCmd.length);
        } else {
            this.versionCmd = null;
        }
    }

    /**
     * See {@link #setVersionCmd(String[])}.
     *
     * @param versionCmdString New value as multi-line string.
     */
    @DataBoundSetter
    public void setVersionCmdString(String versionCmdString) {
        setVersionCmd(Util.fixNull(versionCmdString).split("\n"));
    }

    private static String[] fixEmpty(String[] l) {
        if (l == null || l.length == 0) {
            return null;
        }
        return l;
    }

    /**
     * The regular expression used to parse the output from running
     * {@link #getVersionCmd()}.
     *
     * @return The regex that was set, or null if not set to a valid value.
     */
    public Pattern getVersionPattern() {
        return versionPattern;
    }

    /**
     * Sets {@link #getVersionPatternString()} and {@link #getVersionPattern()}.
     *
     * @param versionPattern New value.
     */
    public void setVersionPattern(Pattern versionPattern) {
        this.versionPattern = versionPattern;
        this.versionPatternString = null;
    }

    /**
     * The regular expression used to parse the output from running
     * {@link #getVersionCmd()}.
     *
     * @return The regex that was set, or null if not set.
     */
    @CheckForNull
    public String getVersionPatternString() {
        if (versionPattern != null) {
            return Util.fixEmpty(versionPattern.pattern());
        }
        return Util.fixEmpty(versionPatternString);
    }

    /**
     * Sets {@link #getVersionPatternString()} and {@link #getVersionPattern()}.
     *
     * @param versionPatternString New value.
     */
    @DataBoundSetter
    public void setVersionPatternString(String versionPatternString) {
        if (Util.fixEmpty(versionPatternString) != null) {
            try {
                this.versionPattern = Pattern.compile(versionPatternString);
                this.versionPatternString = null;
            } catch (PatternSyntaxException ex) {
                this.versionPattern = null;
                this.versionPatternString = versionPatternString;
            }
        } else {
            this.versionPattern = null;
            this.versionPatternString = null;
        }
    }

    /**
     * The minimum version acceptable.
     *
     * @return The version that was set, or null if not set.
     */
    @CheckForNull
    public String getVersionMin() {
        return versionMin;
    }

    /**
     * Sets {@link #getVersionMin()}.
     *
     * @param versionMin New value.
     */
    @DataBoundSetter
    public void setVersionMin(String versionMin) {
        this.versionMin = versionMin;
    }


    /**
     * The maximum version acceptable.
     *
     * @return The version that was set, or null if not set.
     */
    @CheckForNull
    public String getVersionMax() {
        return versionMax;
    }

    /**
     * Sets {@link #getVersionMax()}.
     *
     * @param versionMax New value.
     */
    @DataBoundSetter
    public void setVersionMax(String versionMax) {
        this.versionMax = versionMax;
    }

    @Override
    public FilePath performInstallation(@Nonnull ToolInstallation tool, @Nonnull Node node,
                                        @CheckForNull TaskListener log) throws IOException, InterruptedException {
        final String exePath = getExecutablePath();
        if (exePath == null) {
            throw new IllegalArgumentException(Messages.IsAlreadyInGivenDirectory_executablePathIsEmpty());
        }
        final FilePath executablePath = findExecutableOnNodeOrThrow(exePath, node, log);
        final FilePath parent = executablePath.getParent();
        if (parent == null) {
            // This shouldn't happen, hence not localized.
            throw new IllegalStateException(
                    "Directory" + executablePath + "' has no parent folder");
        }
        final String relPathOrNull = getRelativePath();
        final FilePath resultToReturn;
        if (relPathOrNull == null || relPathOrNull.equals(".")) {
            resultToReturn = parent;
        } else {
            resultToReturn = parent.child(relPathOrNull);
        }
        final String[] vCmd = getVersionCmd();
        final Pattern vPattern = getVersionPattern();
        final String vMax = getVersionMax();
        final String vMin = getVersionMin();
        if (vCmd != null && vPattern != null && (vMin != null || vMax != null)) {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            final Launcher launcher = node.createLauncher(log);
            runCommandOnNode(launcher, resultToReturn, vCmd, output);
            final String cmdOutput = output.toString(StandardCharsets.UTF_8.name());
            final String parsedVersion = VersionChecker.parseVersionCmdOutputForVersion(vPattern, cmdOutput);
            final int versionComparisonResult = VersionChecker.checkVersionIsInRange(vMin, vMax, parsedVersion);
            if (versionComparisonResult != 0) {
                throw new WrongVersionException(exePath, resultToReturn.getRemote(), parsedVersion, vMin,
                        vMax);
            }
        }
        return resultToReturn;
    }

    @Nonnull
    private FilePath findExecutableOnNodeOrThrow(@Nonnull final String exePath, @Nonnull Node node,
                                                 @CheckForNull final TaskListener logOrNull) throws IOException, InterruptedException {
        final FilePath rootPath = node.getRootPath();
        if (rootPath == null) {
            throw new IllegalStateException(Messages.IsAlreadyInGivenDirectory_agentIsOffline());
        }
        final FindInDirCallable nodeOperation = mkCallable(exePath, logOrNull);
        final String absolutePathToExecutable = rootPath.act(nodeOperation);
        final FilePath executablePath = node.createPath(absolutePathToExecutable);
        if (executablePath == null) {
            throw new IllegalStateException(Messages.IsAlreadyInGivenDirectory_agentIsOffline());
        }
        return executablePath;
    }

    // package access for test purposes only
    @Restricted(NoExternalUse.class)
    void runCommandOnNode(final Launcher launcher, final FilePath pwd, final String[] cmd,
                          final OutputStream output) throws IOException, InterruptedException {
        launcher.launch().cmds(cmd).stdout(output).pwd(pwd).join();
    }

    // package access for test purposes only
    @Restricted(NoExternalUse.class)
    @Nonnull
    FindInDirCallable mkCallable(@Nonnull final String executablePath, @CheckForNull final TaskListener logOrNull) {
        return new FindInDirCallable(executablePath, logOrNull);
    }

    /**
     * Descriptor for {@link IsAlreadyInGivenDirectory}.
     */
    @Extension
    @Symbol("findinspecifieddir")
    public static class DescriptorImpl extends ToolInstallerDescriptor<IsAlreadyInGivenDirectory> {
        public String getDisplayName() {
            return Messages.IsAlreadyInGivenDirectory_DescriptorImpl_displayName();
        }

        public FormValidation doCheckExecutablePath(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckVersionCmdString(@QueryParameter String value) {
            if (Util.fixEmpty(value) == null) {
                return FormValidation.ok(Messages.IsAlreadyInGivenDirectory_noVersionValidation());
            }
            if (value.contains(" ") && !value.contains("\n")) {
                return FormValidation.warning(Messages.IsAlreadyInGivenDirectory_versionCmdContainsSpaceButHasNoArguments());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckVersionPatternString(@QueryParameter String versionCmdString,
                                                          @QueryParameter String versionPatternString) {
            if (Util.fixEmpty(versionCmdString) == null) {
                return FormValidation.ok();
            }
            if (Util.fixEmpty(versionPatternString) == null) {
                return FormValidation.error(Messages.IsAlreadyInGivenDirectory_versionPatternIsEmpty());
            }
            try {
                Pattern.compile(versionPatternString);
            } catch (PatternSyntaxException ex) {
                return FormValidation.error(ex,
                        Messages.IsAlreadyInGivenDirectory_versionPatternIsInvalid(versionPatternString));
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckVersionMin(@QueryParameter String versionCmdString,
                                                @QueryParameter String versionPatternString, @QueryParameter String versionMin,
                                                @QueryParameter String versionMax) {
            if (Util.fixEmpty(versionCmdString) != null && Util.fixEmpty(versionPatternString) != null) {
                if (Util.fixEmpty(versionMin) == null && Util.fixEmpty(versionMax) == null) {
                    return FormValidation.error(Messages.IsAlreadyInGivenDirectory_versionMinMaxNotSpecified());
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckVersionMax(@QueryParameter String versionMin, @QueryParameter String versionMax) {
            if (Util.fixEmpty(versionMin) != null && Util.fixEmpty(versionMax) != null) {
                final int cmp = VersionChecker.compareVersions(versionMin, versionMax);
                if (cmp > 0) {
                    return FormValidation.error(Messages.IsAlreadyInGivenDirectory_versionMaxMustNotBeLessThanMinimum(versionMin));
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckVersionTestString(@QueryParameter String versionTestString,
                                                       @QueryParameter String versionCmdString, @QueryParameter String versionPatternString,
                                                       @QueryParameter String versionMin, @QueryParameter String versionMax) {
            if (Util.fixEmpty(versionTestString) == null) {
                return FormValidation.ok();
            }
            if (Util.fixEmpty(versionCmdString) == null) {
                return FormValidation.warning(Messages.IsAlreadyInGivenDirectory_noVersionValidation() + "\n"
                        + Messages.IsAlreadyInGivenDirectory_versionCmdIsEmpty());
            }
            if (Util.fixEmpty(versionPatternString) == null) {
                return FormValidation.warning(Messages.IsAlreadyInGivenDirectory_noVersionValidation() + "\n"
                        + Messages.IsAlreadyInGivenDirectory_versionPatternIsEmpty());
            }
            if (Util.fixEmpty(versionMin) == null) {
                if (Util.fixEmpty(versionMax) == null) {
                    return FormValidation.warning(Messages.IsAlreadyInGivenDirectory_noVersionValidation() + "\n"
                            + Messages.IsAlreadyInGivenDirectory_versionMinMaxNotSpecified());
                }
            }
            final Pattern versionPattern;
            try {
                versionPattern = Pattern.compile(versionPatternString);
            } catch (PatternSyntaxException ex) {
                return FormValidation.warning(ex, Messages.IsAlreadyInGivenDirectory_noVersionValidation() + "\n" +
                        Messages.IsAlreadyInGivenDirectory_versionPatternIsInvalid(versionPatternString));
            }
            final String parsedVersion = VersionChecker.parseVersionCmdOutputForVersion(versionPattern, versionTestString);
            if (Util.fixEmpty(parsedVersion) == null) {
                return FormValidation.warning(Messages.IsAlreadyInGivenDirectory_versionPatternDidNotMatch());
            }
            final int versionComparisonResult = VersionChecker.checkVersionIsInRange(versionMin, versionMax, parsedVersion);
            if (versionComparisonResult < 0) {
                return FormValidation.warning(Messages.IsAlreadyInGivenDirectory_versionIsTooLow(parsedVersion, versionMin));
            }
            if (versionComparisonResult > 0) {
                return FormValidation.warning(Messages.IsAlreadyInGivenDirectory_versionIsTooHigh(parsedVersion, versionMax));
            }
            return FormValidation.ok(Messages.IsAlreadyInGivenDirectory_versionIsOk(parsedVersion));
        }
    }
}