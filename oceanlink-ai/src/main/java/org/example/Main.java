package org.example;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        List<Term> terms = HanLP.segment("查看FT-548484的主图");
        System.out.println(terms);
    }
}