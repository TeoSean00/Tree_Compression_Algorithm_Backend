package com.example.demo.services;

import java.io.IOException;

public abstract class Utility {
  private Utility delegate;

  public abstract void Compress(int[][][] pixels, String outputFileName) throws IOException;

  public abstract int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException;

  public abstract Utility createUtility();

  public void delegateTo(String className) throws Exception {
    this.delegate = (Utility) Class.forName(className).getDeclaredConstructor().newInstance();
  }
}
