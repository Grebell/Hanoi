package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Stack;

public class TowersClient {
    // DO NOT MODIFY THE CODE IN THIS FUNCTION (i kinda needed to? so i could add the rule argument)
    public static void main(String[] args) throws InterruptedException {
        // Default number of discs to 5 unless command-line argument present
        int numDiscs = args.length == 3 ? testInputValue("Disc Number", args[1], 3, 15, 5) : 5;
        // Default delay to 1000 (1 second) unless command-line argument present
        int delay = args.length == 3 ? testInputValue("Delay", args[2], 50, 5000, 1000) : 1000;
        // Start program
        new TowersClient().start(numDiscs, delay, args[0]);
    }

     static String builder(Stack<Integer> arr1, Stack<Integer> arr3, Stack<Integer> arr2) {
        //build arr1section
        String ret = "";
         for (Stack<Integer> integers : Arrays.asList(arr1, arr2, arr3)) {
             ret = sizer(integers, ret);
         }
         if (ret.endsWith(";")) {
             ret = ret.substring(0, ret.length() - 1);
         }
         return ret;
    }

    private static String sizer(Stack<Integer> arr1, String ret) {
        ret += arr1.size() + ":";
        StringBuilder retBuilder = new StringBuilder(ret);
        for (Integer disc : arr1) {
            int i = disc;
            if (i > 0) retBuilder.append(disc).append(",");
        }
        ret = retBuilder.toString();
        if (ret.endsWith(",")) ret = ret.substring(0, ret.length() - 1);
        ret += ";";
        return ret;
    }

    // DO NOT MODIFY THE CODE IN THIS FUNCTION
    private static int testInputValue(String name, String input, int min, int max, int def) {
        int value = 0;
        try {
            if (input == null) {
                System.out.println("INPUT ERROR: Invalid Command-Line Argument.\n");
                System.exit(-1);
            }
            value = Integer.parseInt(input);
        } catch (NumberFormatException nfe) {
            System.out.println(String.format("INPUT ERROR: The Command-Line Argument for %s is Not Numeric.\n", name));
            System.exit(-1);
        }
        if (value < min || value > max) {
            System.out.println(String.format("INPUT ERROR: The Command-Line Argument for %s is Out of Range [%d to %d]. Setting %s to Default Value of %d.\n", name, min, max, name, def));
            value = def;
        }
        return value;
    }

    void move(int numDiscs, int from, int to, int aux, Stack<Integer> arr1, Stack<Integer> arr2, Stack<Integer> arr3, int delay) throws InterruptedException {
        if (numDiscs == 0) return;
        move(numDiscs - 1, from, aux, to, arr1, arr2, arr3, delay);
        Integer i;
        switch (from) {
            case 1:
                i = arr1.pop();
                break;
            case 2:
                i = arr2.pop();
                break;
            case 3:
                i = arr3.pop();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + from);
        }
        switch (to) {
            case 1:
                arr1.push(i);
                break;
            case 2:
                arr2.push(i);
                break;
            case 3:
                arr3.push(i);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + to);
        }
        transmitDataToServer(builder(arr1, arr2, arr3));
        Thread.sleep(delay); // Pause for a short delay
        move(numDiscs - 1, aux, to, from, arr1, arr2, arr3, delay);
    }

    //iterative
    private void makeMove(Stack<Integer> from, Stack<Integer> to) {
        //When pole 1 is empty
        if (from.isEmpty()) from.push(to.pop());
        else if (to.isEmpty()) to.push(from.pop());
        else if (from.peek() > to.peek()) from.push(to.pop());
        else to.push(from.pop());
    }

    private void preparemove(int num_of_disks, Stack<Integer> src, Stack<Integer> aux, Stack<Integer> dest, int delay) throws InterruptedException {
        int totalMoves = (int) (Math.pow(2, num_of_disks) - 1);
        for (int x = 1; x <= totalMoves; x++) {
            switch (x % 3) {
                case 0:
                    makeMove(aux, dest);
                    transmitDataToServer(builder(src, aux, dest));
                    Thread.sleep(delay);
                    break;
                case 1:
                    makeMove(src, dest);
                    transmitDataToServer(builder(src, aux, dest));
                    Thread.sleep(delay);
                    break;
                case 2:
                    makeMove(src, aux);
                    transmitDataToServer(builder(src, aux, dest));
                    Thread.sleep(delay);
                    break;
            }
        }
    }
    public void start(int numDiscs, int delay, String rule) {
        rule = rule.toLowerCase();
        Stack<Integer> arr1 = new Stack<Integer>();
        Stack<Integer> arr2 = new Stack<Integer>();
        Stack<Integer> arr3 = new Stack<Integer>();
        for (int i = numDiscs; i > 0; i--) arr1.push(i);

        try {
            // Send RESET message to initialize the display on the server
            transmitDataToServer(String.format("RESET:%d", numDiscs));
            Thread.sleep(delay); // Pause for a short delay
            // Loop through each of the Towers example movements
            if (rule.equals("recursive")) {
                int from = 1;
                int to = 2;
                int aux = 3;
                move(numDiscs, from, to, aux, arr1, arr2, arr3, delay);
            }
            else if (rule.equals("iterative")) preparemove(numDiscs, arr1, arr2, arr3, delay);
            else System.out.println("no rule given");

            // Send STOP message to inform the server the client is done communicating
            transmitDataToServer("STOP");
        } catch (InterruptedException ie) {
            System.out.printf("THREAD ERROR: (%s)\n%n", ie.getMessage());
        }
    }

    // DO NOT MODIFY THE CODE IN THIS FUNCTION
    private String transmitDataToServer(String message) {
        Socket client = null;
        try {
            client = new Socket("localhost", 4930);
            new PrintWriter(client.getOutputStream(), true).println(message);
            return new BufferedReader(new InputStreamReader(client.getInputStream())).readLine();
        } catch (UnknownHostException uhe) {
            System.out.println(String.format("COMM ERROR: (%s)\n", uhe.getMessage()));
        } catch (IOException ioe) {
            System.out.println(String.format("COMM ERROR: (%s)\n", ioe.getMessage()));
        } finally {
            try {
                if (client != null) {
                    client.close();
                }
            } catch (IOException ioe) {
                // Nothing to do here
            }
        }
        return "ERROR";
    }
}