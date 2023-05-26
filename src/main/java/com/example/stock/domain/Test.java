package com.example.stock.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Test {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String[] input = br.readLine().split(" ");
        int hours = Integer.parseInt(input[0]);
        int minute = Integer.parseInt(input[1]);

        if(hours==0){
            hours=24;
        }
        int hoursToMinute = hours*60;

        int sum = hoursToMinute+minute;

        int newHours = (sum-45)/60;
        int newMinute = (sum-45)%60;

        if(newHours==24 && newMinute>=0){
            newHours=0;
        }
        System.out.println(newHours+" "+newMinute);
    }
}
