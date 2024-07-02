package myfirstmodule.impl;

public class test {
    public static void main(String[] args) {
        System.out.println("Hello World!");

        // bytes=0- or bytes=0-1023
        var rangeHeader = "bytes=0-";
        var range = rangeHeader.replace("bytes=", "").split("-");
        var start = Integer.parseInt(range[0]);
        var end = range.length > 1 && !range[1].isEmpty() ? Integer.parseInt(range[1]) : Integer.MAX_VALUE;
        System.out.println("Start: " + start + ", End: " + end);
        System.out.println("Content-Length " + Integer.toString(end - start));
    }
}
