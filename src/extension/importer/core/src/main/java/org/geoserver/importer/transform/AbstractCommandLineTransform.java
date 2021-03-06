/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang.SystemUtils;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

/**
 * Generic file translator getting a set of options, an input file, and an output file
 * 
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractCommandLineTransform extends AbstractTransform implements
        PreTransform {

    private static final long serialVersionUID = 5998049960852782644L;

    static final long DEFAULT_TIMEOUT = 60 * 60 * 1000; // one hour

    List<String> options;

    public AbstractCommandLineTransform(List<String> options) {
        this.options = options;
    }

    /**
     * @return the options
     */
    public List<String> getOptions() {
        return options;
    }

    /**
     * @param options the options to set
     */
    public void setOptions(List<String> options) {
        this.options = options;
    }

    @Override
    public boolean stopOnError(Exception e) {
        return true;
    }

    @Override
    public void apply(ImportTask task, ImportData data) throws Exception {
        boolean inline = isInline();
        File executable = getExecutable();
        Resource inputFile = getInputFile(data);
        Map<String, File> substitutions = new HashMap<>();
        substitutions.put("input", inputFile.file());
        Resource outputDirectory = null;
        Resource outputFile = null;
        if (!inline) {
            outputDirectory = getOutputDirectory(data);
            outputFile = outputDirectory.get(inputFile.name());
            substitutions.put("output", outputFile.file());
        }


        // setup the options
        CommandLine cmd = new CommandLine(executable);
        cmd.setSubstitutionMap(substitutions);

        setupCommandLine(inline, cmd);

        // prepare to run
        DefaultExecutor executor = new DefaultExecutor();
        // make sure we don't try to execute for too much time
        executor.setWatchdog(new ExecuteWatchdog(DEFAULT_TIMEOUT));

        // grab at least some part of the outputs
        int limit = 16 * 1024;
        try {
            try (OutputStream os = new BoundedOutputStream(new ByteArrayOutputStream(), limit);
                    OutputStream es = new BoundedOutputStream(new ByteArrayOutputStream(), limit)) {
                PumpStreamHandler streamHandler = new PumpStreamHandler(os, es);
                executor.setStreamHandler(streamHandler);
                try {
                    int result = executor.execute(cmd);
                    
                    if (executor.isFailure(result)) {
                        // toString call is routed to ByteArrayOutputStream, which does the right string
                        // conversion
                        throw new IOException("Failed to execute command " + cmd.toString()
                                + "\nStandard output is:\n" + os.toString() + "\nStandard error is:\n"
                                + es.toString());
                    }
                } catch (Exception e) {
                    throw new IOException("Failure to execute command " + cmd.toString() + "\nStandard output is:\n" + os.toString() + "\nStandard error is:\n"
                            + es.toString(), e);
                }
            }

            // if not inline, replace inputs with output
            if (!inline) {
                List<String> names = getReplacementTargetNames(data);
                Resource inputParent = inputFile.parent();
                for (String name : names) {
                    Resource output = outputDirectory.get(name);
                    Resource input = inputParent.get(name);
                    if (Resources.exists(output)) {
                        // uses atomic rename on *nix, delete and copy on Windows
                        output.renameTo(input);
                    } else if (Resources.exists(input)) {
                        input.delete();
                    }
                }
            }
        } finally {
            if (outputDirectory != null) {
                outputDirectory.delete();
            }
        }
    }

    protected boolean checkAvailable() throws IOException {
        try {
            CommandLine cmd = new CommandLine(getExecutable());
            for (String option : getAvailabilityTestOptions()) {
                cmd.addArgument(option);
            }

            // prepare to run
            DefaultExecutor executor = new DefaultExecutor();

            // grab at least some part of the outputs
            int limit = 16 * 1024;
            try (OutputStream os = new BoundedOutputStream(new ByteArrayOutputStream(), limit);
                    OutputStream es = new BoundedOutputStream(new ByteArrayOutputStream(), limit)) {
                PumpStreamHandler streamHandler = new PumpStreamHandler(os, es);
                executor.setStreamHandler(streamHandler);
                int result = executor.execute(cmd);

                if (result != 0) {
                    LOGGER.log(Level.SEVERE, "Failed to execute command " + cmd.toString()
                            + "\nStandard output is:\n" + os.toString() + "\nStandard error is:\n"
                            + es.toString());
                    return false;
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failure to execute command " + cmd.toString(), e);
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failure to locate executable for class " + this.getClass(), e);
            return false;
        }

        return true;
    }

    /**
     * Returns the list of options to be passed the executable to test its availability and ability
     * to run. e.g. "--help"
     * 
     *
     */
    protected abstract List<String> getAvailabilityTestOptions();

    protected void setupCommandLine(boolean inline, CommandLine cmd) {
        for (String option : options) {
            cmd.addArgument(option, false);
        }

        // setup input and output files
        if (inline) {
            cmd.addArgument("${input}", false);
        } else {

            if (isOutputAfterInput()) {
                cmd.addArgument("${input}", false);
                cmd.addArgument("${output}", false);
            } else {
                cmd.addArgument("${output}", false);
                cmd.addArgument("${input}", false);
            }
        }
    }

    /**
     * Returns the name of all the files that should be transferred from input to output (sometimes
     * the output is made of several files)
     * 
     * @param data
     *
     * @throws IOException
     */
    protected abstract List<String> getReplacementTargetNames(ImportData data) throws IOException;

    /**
     * Returns true if the command line manipulates the input file directly
     * 
     *
     */
    protected boolean isInline() {
        return false;
    }

    /**
     * Returns true if in the command line the output file comes after the input one. The default
     * implementation returns true
     * 
     *
     */
    protected boolean isOutputAfterInput() {
        return true;
    }

    /**
     * The command input file
     * 
     * @param data
     *
     * @throws IOException
     */
    protected abstract Resource getInputFile(ImportData data) throws IOException;

    /**
     * The directory used for outputs, by default, a subdirectory of the input file parent
     * 
     * @param data
     *
     * @throws IOException
     */
    protected Resource getOutputDirectory(ImportData data) throws IOException {
        Resource input = getInputFile(data);
        Resource parent = input.parent();
        Resource tempFile = Resources.createRandom("tmp", null, parent);
        tempFile.delete();

        return tempFile;
    }

    /**
     * Implementors must provide the executable to be run
     * 
     *
     */
    protected abstract File getExecutable() throws IOException;

    /**
     * Locates and executable in the system path. On windows it will automatically append .exe to
     * the searched file name
     * 
     * @param name
     *
     * @throws IOException
     */
    protected File getExecutableFromPath(String name) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            name = name + ".exe";
        }
        String systemPath = System.getenv("PATH");
        if (systemPath == null) {
            systemPath = System.getenv("path");
        }
        if (systemPath == null) {
            throw new IOException("Path is not set, cannot locate " + name);
        }
        String[] paths = systemPath.split(File.pathSeparator);

        for (String pathDir : paths) {
            File file = new File(pathDir, name);
            if (file.exists() && file.isFile() && file.canExecute()) {
                return file;
            }
        }
        throw new IOException(
                "Could not locate executable (or could locate, but does not have execution rights): "
                        + name);
    }

    /**
     * Output stream wrapper with a soft limit
     * 
     * @author Andrea Aime - GeoSolutions
     */
    static final class BoundedOutputStream extends CountingOutputStream {

        private long maxSize;

        private OutputStream delegate;

        public BoundedOutputStream(OutputStream delegate, long maxSize) {
            super(delegate);
            this.delegate = delegate;
            this.maxSize = maxSize;
        }

        @Override
        public void write(byte[] bts) throws IOException {
            if (getByteCount() > maxSize) {
                return;
            }
            super.write(bts);
        }

        @Override
        public void write(byte[] bts, int st, int end) throws IOException {
            if (getByteCount() > maxSize) {
                return;
            }
            super.write(bts, st, end);
        }

        @Override
        public void write(int idx) throws IOException {
            if (getByteCount() > maxSize) {
                return;
            }
            super.write(idx);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

    }

}
