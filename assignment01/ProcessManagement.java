/* Programming Assignment 1
 * Author: Yap Wei Lok
 * ID: 1002394
 * Date: 01/02/2018
 */

import java.io.File;
import java.util.*;

public class ProcessManagement {
    // set the working directory and instructions file
    private static File workingDirectory = new File("/Users/ericyap/Dropbox/SUTD" +
            "/50.005_computer_system_engineering" +
            "/week02/assignment01/submit/src/test_folder/graph-file3");
    private static File instructionSet = new File("graph-file3");
    // set thread sleep duration in ms (for concurrency testing and better visualization)
    private static long sleepDuration = 0;

    // DO NOT EDIT VARIABLES BELOW THIS LINE
    // ========================================= //

    // set of threads to look after (ie. not yet run or still running)
    private static Set<ProcessThread> threads = new HashSet<>();
    // set of terminated nodes with terminated threads
    private static Set<ProcessGraphNode> terminatedNodes = new HashSet<>();

    public static void main(String[] args) {
        parseArgs(args);
        initGraphThreads();

        boolean success = manageThreads();
        printFinalMessage(success);
    }

    /**
     * Schedule the processes while all nodes are not done executing
     */
    private static boolean manageThreads() {
        while (!allNodesFinished()) {
            for (Iterator<ProcessThread> iter = threads.iterator(); iter.hasNext(); ) {
                ProcessThread pThread = iter.next();
                ProcessGraphNode node = pThread.getNode();
                // set node to done if thread finished successfully
                // remove the thread from observed threads
                if (pThread.getFinishStatus() == FinishStatus.FINISHED) {
                    node.setDone();
                    iter.remove();
                }

                // exit manageThreads() if the finish status is TERMINATED
                // check that other running threads have finished before terminating
                if (pThread.getFinishStatus() == FinishStatus.TERMINATED) {
                    node.setTerminated(pThread.getTerminationMsg());
                    terminatedNodes.add(node);
                    if (!someNodesStillRunning()) {
                        return false;
                    }
                }

                /* set node to runnable if all parents finished execution
                and node not already executed or is done or is terminated due to error */
                if (node.allParentsDone() && !node.isExecuted()
                        && !node.isDone() && !node.isTerminated())
                    node.setRunnable();

                // start the thread and set node to executed if it is runnable
                if (node.isRunnable()) {
                    pThread.start();
                    node.setExecuted();
                }
            }
        }

        return true;
    }

    /**
     * Parse the command line arguments if they are provided
     *
     * @param args - the command line arguments
     */
    private static void parseArgs(String[] args) {
        // no arguments
        if (args.length == 0)
            return;

        // wrong number of arguments
        if (args.length != 2 && args.length != 3) {
            throw new IllegalArgumentException("Wrong number of arguments.");
        }

        // temporary working directory and instruction set to the arguments provided
        File tempWorkingDirectory = new File(args[0]);
        File tempInstructionSet = new File(args[1]);
        // provided path is not a directory
        if (!tempWorkingDirectory.isDirectory()) {
            throw new IllegalArgumentException(tempWorkingDirectory + " is not a directory.");
        }

        File file = new File(tempWorkingDirectory + "/" + tempInstructionSet);
        // file does not exist
        if (!file.exists()) {
            throw new IllegalArgumentException("The file provided does not exist.");
        }
        // path given is not a file
        if (!file.isFile()) {
            throw new IllegalArgumentException("The path provided is not a file.");
        }

        // set working directory and instruction set
        workingDirectory = tempWorkingDirectory;
        instructionSet = tempInstructionSet;

        // last argument is the sleep duration
        if (args.length == 3) {
            sleepDuration = Integer.parseInt(args[2]);
        }
    }

    /**
     * Check if all nodes have finished execution
     *
     * @return true if all nodes finished, else false
     */
    private static boolean allNodesFinished() {
        return threads.isEmpty();
    }

    /**
     * Check if there are still nodes running
     *
     * @return true if some nodes are still running, else false
     */
    private static boolean someNodesStillRunning() {
        for (ProcessGraphNode node : ProcessGraph.nodes.values()) {
            if (node.isExecuted() && !node.isDone() && !node.isTerminated())
                return true;
        }

        return false;
    }

    /**
     * Function to print out the final messages
     * If there's error print out the thread termination messages
     */
    private static void printFinalMessage(boolean success) {
        if (!success) {
            System.out.println();
            // Program terminating pre-maturely
            for (ProcessGraphNode node : terminatedNodes) {
                String msg = String.format("Process %d terminated with an error: %s",
                        node.getNodeId(), node.getTerminationMsg());
                System.out.println(msg);
            }
            System.out.println("ProcessManagement terminating due to the above error(s).");
            return;
        }

        // Print success finish status
        System.out.println();
        System.out.println("All processes finished successfully.");
    }

    /**
     * Initialize the graph and print it out
     */
    private static void initGraphThreads() {
        // parse the instruction file and construct a data structure, stored inside ProcessGraph class
        ParseFile.generateGraph(new File(workingDirectory + "/" + instructionSet));
        // print the graph
        ProcessGraph.printGraph();

        // initialize all threads
        for (ProcessGraphNode node : ProcessGraph.nodes.values())
            threads.add(new ProcessThread(node, workingDirectory, sleepDuration));
    }
}