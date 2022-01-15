package org.javacs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class MainArgs {

    public boolean help = false;
    public String workspace = "";
    public List<String> specialMethods = new ArrayList<String>();
    public String outputJsonFile = "";
    public int depth = -1;

    private Options options = new Options();

    public MainArgs(String[] args) {
        initOptions();
        parse(args);
    }

    public void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java-find-reference", options);        
    }

    private void initOptions() {
        Option help = new Option("help", false, "print this message");
        Option workspace = Option.builder("workspace").argName("dir").hasArg().required(true)
                .desc("Workspace to parse the code. For maven project, input the directory contains the pom.xml.")
                .build();
        Option specialMethods = Option.builder("specialMethods").argName("method").hasArg().desc(
                "The special methods which are leaves by will be executed by all methods like setUp, tearDown in Junit, split with comma(,).")
                .build();
        Option outputJsonFile = Option.builder("outputJson").argName("file").hasArg().desc("Output to json file")
                .build();
        Option searchDepth = Option.builder("searchDepth").argName("depth").hasArg().desc(
                "Search reference depth. 0: don't find reference just care about the function contains the change, -1 search the code until highest layer.")
                .build();

        options.addOption(help);
        options.addOption(workspace);
        options.addOption(specialMethods);
        options.addOption(outputJsonFile);
        options.addOption(searchDepth);
    }

    private void parse(String[] args) {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);
            if(line.hasOption("help")) {
                this.help = true;
            }
            if(line.hasOption("workspace")) {
                this.workspace = line.getOptionValue("workspace");
            }
            if(line.hasOption("specialMethods")) {
                this.specialMethods.addAll(Arrays.asList(line.getOptionValue("specialMethods").split(",")));
            }
            if(line.hasOption("outputJson")) {
                this.outputJsonFile = line.getOptionValue("outputJson");
            }
            if(line.hasOption("searchDepth")) {
                this.depth = Integer.parseInt(line.getOptionValue("searchDepth"));
            }
        }
        catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }
    }
}
