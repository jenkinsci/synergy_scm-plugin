/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author u48jfe
 */
public class GenericCommand extends Command{

  List<String> result = new ArrayList<String>();
  private final List<String> args = new ArrayList<>();

  public GenericCommand(String[] p_args) {
  args.addAll(Arrays.asList(p_args));
  }
  
  
  
  @Override
  public String[] buildCommand(String ccmExe) {
    List<String> l_return = new ArrayList<>();
    l_return.add(ccmExe);
    l_return.addAll(args);
    return l_return.toArray(new String[0]);
  }

  @Override
  public void parseResult(String result) {
    if (result != null) {
      try {
        BufferedReader reader = new BufferedReader(new StringReader(result));
        String line = reader.readLine();
        while (line != null) {
          line = line.trim();
          if (line.length() != 0) {
            this.result.add(line);
          }
          line = reader.readLine();
        }
        
      } catch (IOException e) {
        // Should not happen with a StringReader.
      }
      
    }
    
  }
  
  public List<String> getresult() {
    return result;
  }
  
}
