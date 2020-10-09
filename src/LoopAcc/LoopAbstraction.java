// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package LoopAcc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * This class takes a file and changes all of the loops in a advanced abstraction to make the
 * program verifiable for specific cpa's
 */
public class LoopAbstraction {
  private int lineNumber = 1;


  public LoopAbstraction() {

  }

  /**
   * This method changes all the necessary lines of codes and saves it in a new file
   *
   * @param loopInfo Information about all the loops in the file
   */
  public void
      changeFileToAbstractFile(
          LoopInformation loopInfo,
          LogManager logger,
          String pathForNewFile,
          String abstractionLevel,
          boolean automate,
          boolean onlyAccL) {
    List<LoopData> outerLoopTemp = new ArrayList<>();
    List<Integer> loopStarts = new ArrayList<>();
    List<String> preUsedVariables = new ArrayList<>();
    boolean closed = true;
    for (LoopData loopData : loopInfo.getLoopData()) {
      if (loopData.getLoopType().equals("while")) {
        loopStarts.add(
            loopData.getLoopStart().getEnteringEdge(0).getFileLocation().getStartingLineInOrigin());
      } else if (loopData.getLoopType().equals("for")) {
        loopStarts.add(
            loopData.getLoopStart().getEnteringEdge(0).getFileLocation().getStartingLineInOrigin()
        );
      }
    }

    String fileLocation = "../cpachecker/" + loopInfo.getCFA().getFileNames().get(0).toString();

    String content =
        "extern void __VERIFIER_error() __attribute__ ((__noreturn__));" + System.lineSeparator();

    boolean flagInt = true;
    boolean flaguInt = true;
    boolean flagChar = true;
    boolean flaguChar = true;
    boolean flagShort = true;
    boolean flaguShort = true;
    boolean flagLong = true;
    boolean flaguLong = true;
    boolean flagLongLong = true;
    boolean flagDouble = true;
    for (LoopData lD : loopInfo.getLoopData()) {
      for (String io : lD.getInputsOutputs()) {
        switch (io.split("&")[1]) {
          case "int":
          case "signed int":
            if (flagInt) {
              content +=
                  "extern unsigned int __VERIFIER_nondet_int(void);" + System.lineSeparator();
              content += "extern void __VERIFIER_assume(int cond);" + System.lineSeparator();
              flagInt = false;
            }
            break;
          case "unsigned int":
            if (flaguInt) {
            content += "extern unsigned int __VERIFIER_nondet_uint(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(unsigned int cond);" + System.lineSeparator();
            flagInt = false;
          }
            break;
          case "char":
          case "signed char":
            if (flagChar) {
            content += "extern char __VERIFIER_nondet_char(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(char cond);" + System.lineSeparator();
            flagChar = false;
          }
            break;
          case "unsigned char":
            if (flaguChar) {
              content += "extern char __VERIFIER_nondet_uchar(void);" + System.lineSeparator();
              content +=
                  "extern void __VERIFIER_assume(unsigned char cond);" + System.lineSeparator();
              flagChar = false;
            }
            break;
          case "short":
          case "signed short":
            if (flagShort) {
            content += "extern short __VERIFIER_nondet_short(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(short cond);" + System.lineSeparator();
            flagShort = false;
          }
            break;
          case "unsigned short":
            if (flaguShort) {
              content += "extern short __VERIFIER_nondet_ushort(void);" + System.lineSeparator();
              content +=
                  "extern void __VERIFIER_assume(unsigned short cond);" + System.lineSeparator();
              flagShort = false;
            }
            break;
          case "long":
          case "signed long":
            if (flagLong) {
            content += "extern long __VERIFIER_nondet_long(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(long cond);" + System.lineSeparator();
            flagLong = false;
          }
          break;
        case "unsigned long":
          if (flaguLong) {
            content += "extern long __VERIFIER_nondet_ulong(void);" + System.lineSeparator();
            content +=
                "extern void __VERIFIER_assume(unsigned long cond);" + System.lineSeparator();
            flagLong = false;
          }
            break;
          case "long double":
            if (flagLongLong) {
            content +=
                "extern long double __VERIFIER_nondet_long_double(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(long double cond);" + System.lineSeparator();
            flagLongLong = false;
          }
            break;
          case "double":
            if (flagDouble) {
            content += "extern double __VERIFIER_nondet_double(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(double cond);" + System.lineSeparator();
            flagDouble = false;
          }
            break;
          case "float":
            if (flagDouble) {
              content += "extern double __VERIFIER_nondet_float(void);" + System.lineSeparator();
              content += "extern void __VERIFIER_assume(float cond);" + System.lineSeparator();
              flagDouble = false;
            }
            break;
        }

      }
    }

    try (FileReader freader = new FileReader(fileLocation)) {
      try (BufferedReader reader = new BufferedReader(freader)) {

      String line = "";
      boolean accFlag = true;

      while (line != null) {
        if (loopStarts.contains(lineNumber) && accFlag) {
          for (LoopData loopD : loopInfo.getLoopData()) {

            if ((loopD
                .getLoopStart()
                    .getEnteringEdge(0)
                    .getFileLocation()
                .getStartingLineInOrigin() == lineNumber
                && loopD.getCanBeAccelerated()
                && onlyAccL)
                || (loopD.getLoopStart()
                    .getEnteringEdge(0)
                    .getFileLocation()
                    .getStartingLineInOrigin() == lineNumber
                    && !onlyAccL)
            ) {

              CFANode endNodeCondition = findLastNodeInCondition(loopD);
              if (loopD.getLoopType().equals("while")) {
                line = reader.readLine();
                line = variablesAlreadyUsed(preUsedVariables, line);
                content = content + whileCondition(loopD, abstractionLevel);
                lineNumber++;
              } else if (loopD.getLoopType().equals("for")) {
                line = reader.readLine();
                line = variablesAlreadyUsed(preUsedVariables, line);
                content = content + forCondition(loopD, preUsedVariables, abstractionLevel);
                lineNumber++;
              }
              content += undeterministicVariables(loopD, preUsedVariables, abstractionLevel);

              if (!loopD.getIsOuterLoop()) {
                if (loopD.getLoopType().equals("while")) {
                content +=
                    ("__VERIFIER_assume(" + loopD.getCondition() + ");") + System.lineSeparator();
                while (lineNumber >= endNodeCondition.getEnteringEdge(
                    0)
                    .getFileLocation()
                    .getEndingLineInOrigin()
                    && line != null
                    && lineNumber <= loopD.getLoopEnd()
                        .getEnteringEdge(0)
                        .getFileLocation()
                        .getEndingLineInOrigin()) {

                  line = reader.readLine();
                  line = variablesAlreadyUsed(preUsedVariables, line);
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                boolean flagTopIf2 = false;
                boolean flagKP2 = false;
                line = reader.readLine();
                line = variablesAlreadyUsed(preUsedVariables, line);
                if (line != null && line.contains("if")) {
                  flagTopIf2 = true;
                  flagKP2 = true;
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                while (!closed) {
                  if (flagTopIf2) {
                  line = reader.readLine();
                  line = variablesAlreadyUsed(preUsedVariables, line);
                }
                flagTopIf2 = true;
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                if (flagKP2 || flagTopIf2) {
                line = reader.readLine();
                line = variablesAlreadyUsed(preUsedVariables, line);
              }
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
                while (line != null && !line.contains("}")) {
                  line = reader.readLine();
                  line = variablesAlreadyUsed(preUsedVariables, line);
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                content +=
                    ("__VERIFIER_assume(!("
                        + loopD.getCondition()
                        + "));"
                        + System.lineSeparator());

                for (int i = outerLoopTemp.size() - 1; i >= 0; i--) {
                  boolean flagIf = false;
                  boolean flagKP = false;
                  line = reader.readLine();
                  line = variablesAlreadyUsed(preUsedVariables, line);
                  if (line != null && line.contains("if")) {
                    flagIf = true;
                    flagKP = true;
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                  while (!closed) {
                    if (flagIf) {
                    line = reader.readLine();
                    line = variablesAlreadyUsed(preUsedVariables, line);
                  }
                  flagIf = true;
                    closed = ifCaseClosed(line, closed);
                    content += line + System.lineSeparator();
                    lineNumber++;
                  }
                  if (flagKP || flagIf) {
                  line = reader.readLine();
                  line = variablesAlreadyUsed(preUsedVariables, line);
                }
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                  while (line != null && !line.contains("}")) {
                    line = reader.readLine();
                    line = variablesAlreadyUsed(preUsedVariables, line);
                    closed = ifCaseClosed(line, closed);
                    content += line + System.lineSeparator();
                    lineNumber++;
                  }
                  if (outerLoopTemp.get(i).getLoopType().equals("for")) {
                    content +=
                        ("__VERIFIER_assume(!("
                            + outerLoopTemp.get(i).getCondition().split(";")[1]
                            + "));"
                            + System.lineSeparator());
                  } else if (outerLoopTemp.get(i).getLoopType().equals("while")) {
                    content +=
                        ("__VERIFIER_assume(!("
                            + outerLoopTemp.get(i).getCondition()
                            + "));"
                            + System.lineSeparator());
                  }
                }
                outerLoopTemp.clear();
              } else if (loopD.getLoopType().equals("for")) {
                content +=
                    ("__VERIFIER_assume("
                        + loopD.getCondition().split(";")[1]
                        + ");")
                        + System.lineSeparator();
                while (lineNumber >= endNodeCondition.getEnteringEdge(0)
                    .getFileLocation()
                    .getStartingLineInOrigin()
                    && line != null
                    && lineNumber <= loopD
                        .getLoopEnd()
                        .getEnteringEdge(0)
                        .getFileLocation()
                        .getEndingLineInOrigin()
                ) {
                  line = reader.readLine();
                  line = variablesAlreadyUsed(preUsedVariables, line);
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                boolean flagIfTop = false;
                boolean flagKP = false;
                line = reader.readLine();
                line = variablesAlreadyUsed(preUsedVariables, line);
                if (line != null && line.contains("if")) {
                  flagIfTop = true;
                  flagKP = true;
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
              }
              while (!closed) {
                if (flagIfTop) {
                  line = reader.readLine();
                  line = variablesAlreadyUsed(preUsedVariables, line);
              }
                flagIfTop = true;
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
              }
              if (flagKP || flagIfTop) {
              line = reader.readLine();
              line = variablesAlreadyUsed(preUsedVariables, line);
            }
              closed = ifCaseClosed(line, closed);
              content += line + System.lineSeparator();
              lineNumber++;

              while (line != null && !line.contains("}")) {
                line = reader.readLine();
                line = variablesAlreadyUsed(preUsedVariables, line);
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
              }
                content +=
                    ("__VERIFIER_assume(!("
                        + loopD.getCondition().split(";")[1]
                        + "));"
                        + System.lineSeparator());

                for (int i = outerLoopTemp.size() - 1; i >= 0; i--) {
                  boolean flagIf = false;
                  boolean flagBotKP = false;
                  line = reader.readLine();
                  line = variablesAlreadyUsed(preUsedVariables, line);
                  if (line != null && line.contains("if")) {
                    flagIf = true;
                    flagBotKP = true;
                    closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                  while (!closed) {
                    if (flagIf) {
                    line = reader.readLine();
                    line = variablesAlreadyUsed(preUsedVariables, line);
                  }
                  flagIf = true;
                    closed = ifCaseClosed(line, closed);
                    content += line + System.lineSeparator();
                    lineNumber++;
                  }
                  if (flagBotKP || flagIf) {
                  line = reader.readLine();
                  line = variablesAlreadyUsed(preUsedVariables, line);
                }
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                  while (line != null && !line.contains("}")) {
                    line = reader.readLine();
                    line = variablesAlreadyUsed(preUsedVariables, line);
                    closed = ifCaseClosed(line, closed);
                    content += line + System.lineSeparator();
                    lineNumber++;
                  }
                  if (outerLoopTemp.get(i).getLoopType().equals("for")) {
                  content +=
                      ("__VERIFIER_assume(!("
                          + outerLoopTemp.get(i)
                              .getCondition()
                              .split(";")[1]
                          + "));"
                          + System.lineSeparator());
                } else if (outerLoopTemp.get(i).getLoopType().equals("while")) {
                  content +=
                      ("__VERIFIER_assume(!("
                          + outerLoopTemp.get(i).getCondition()
                          + "));"
                          + System.lineSeparator());
                }
                }
                outerLoopTemp.clear();
              }
            } else if (loopD.getIsOuterLoop()) {
              if (loopD.getLoopType().equals("while")) {
                content +=
                    ("__VERIFIER_assume(" + loopD.getCondition() + ");") + System.lineSeparator();
              } else if (loopD.getLoopType().equals("for")) {
                content +=
                    ("__VERIFIER_assume(" + loopD.getCondition().split(";")[1] + ");")
                        + System.lineSeparator();
              }
              outerLoopTemp.add(loopD);
              if (loopD.getLoopType().equals("for")) {
              while (lineNumber >= endNodeCondition.getEnteringEdge(0)
                  .getFileLocation()
                  .getEndingLineInOrigin()
                  && line != null
                  && (lineNumber < (loopD
                          .getInnerLoop()
                      .getIncomingEdges()
                      .asList()
                      .get(0)
                      .getFileLocation()
                      .getStartingLineInOrigin()
                  )
                  )
              )
              {
                line = reader.readLine();
                line = variablesAlreadyUsed(preUsedVariables, line);
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
                // lineNumber++;
              }
            } else if (loopD.getLoopType().equals("while")) {
              while (lineNumber >= endNodeCondition.getEnteringEdge(0)
                  .getFileLocation()
                  .getEndingLineInOrigin()
                  && line != null
                  && (lineNumber < (loopD.getInnerLoop()
                      .getIncomingEdges()
                      .asList()
                      .get(0)
                      .getFileLocation()
                      .getStartingLineInOrigin()))) {
                line = reader.readLine();
                line = variablesAlreadyUsed(preUsedVariables, line);
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
              }
            }
            }
          } else {
            accFlag = false;
          }
        }
      } else if (!loopStarts.contains(lineNumber) || !accFlag) {
          accFlag = true;
          line = reader.readLine();
          if (line != null) {
            content += line + System.lineSeparator();
            lineNumber++;
          }
        }
      }
      reader.close();
    }
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Something is not working with the file you try to import");
    }
    printFile(loopInfo, content, pathForNewFile, logger, automate);
  }

  private String undeterministicVariables(
      LoopData loopD,
      List<String> preUsedVariables,
      String abstractionLevel) {
    String tmp = "";
    List<String> variables = new ArrayList<>();
    if (abstractionLevel.equals("naiv")) {
      variables = loopD.getOutputs();
    } else {
      variables = loopD.getInputsOutputs();
    }
    for (String x : variables) {
      if (x.contains("Array")) {
        switch (x.split("&")[1].split(":")[1]) {
          case "int":
          case "signed int":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp +=
                  x.split("&")[1].split(":")[1]
                      + " "
                      + x.split("&")[0]
                      + "["
                      + x.split("&")[1].split(":")[2]
                      + "]"
                      + ";"
                      + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp +=
                "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                    + x.split("&")[1].split(":")[2]
                    + "; __cpachecker_tmp_i++){"
                    + (x.split("&")[0]
                        + "[__cpachecker_tmp_i]"
                        + "=__VERIFIER_nondet_int();}"
                        + System.lineSeparator());
            break;
          case "unsigned int":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp +=
                  x.split("&")[1].split(":")[1]
                      + " "
                      + x.split("&")[0]
                      + "["
                      + x.split("&")[1].split(":")[2]
                      + "]"
                      + ";"
                      + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp +=
                "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                    + x.split("&")[1].split(":")[2]
                    + "; __cpachecker_tmp_i++){"
                    + (x.split("&")[0]
                        + "[__cpachecker_tmp_i]"
                        + "=__VERIFIER_nondet_uint();}"
                        + System.lineSeparator());
            break;
          case "char":
          case "signed char":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp +=
                  x.split("&")[1].split(":")[1]
                      + " "
                      + x.split("&")[0]
                      + "["
                      + x.split("&")[1].split(":")[2]
                      + "]"
                      + ";"
                      + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp +=
                "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                    + x.split("&")[1].split(":")[2]
                    + "; __cpachecker_tmp_i++){"
                    + (x.split("&")[0]
                        + "[__cpachecker_tmp_i]"
                        + "=__VERIFIER_nondet_char();}"
                        + System.lineSeparator());
            break;
          case "unsigned char":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp +=
                  x.split("&")[1].split(":")[1]
                      + " "
                      + x.split("&")[0]
                      + "["
                      + x.split("&")[1].split(":")[2]
                      + "]"
                      + ";"
                      + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp +=
                "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                    + x.split("&")[1].split(":")[2]
                    + "; __cpachecker_tmp_i++){"
                    + (x.split("&")[0]
                        + "[__cpachecker_tmp_i]"
                        + "=__VERIFIER_nondet_uchar();}"
                        + System.lineSeparator());
            break;
          case "short":
          case "signed short":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp +=
                  x.split("&")[1].split(":")[1]
                      + " "
                      + x.split("&")[0]
                      + "["
                      + x.split("&")[1].split(":")[2]
                      + "]"
                      + ";"
                      + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp +=
                "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                    + x.split("&")[1].split(":")[2]
                    + "; __cpachecker_tmp_i++){"
                    + (x.split("&")[0]
                        + "[__cpachecker_tmp_i]"
                        + "=__VERIFIER_nondet_short();}"
                        + System.lineSeparator());
            break;
          case "unsigned short":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp +=
                  x.split("&")[1].split(":")[1]
                      + " "
                      + x.split("&")[0]
                      + "["
                      + x.split("&")[1].split(":")[2]
                      + "]"
                      + ";"
                      + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp +=
                "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                    + x.split("&")[1].split(":")[2]
                    + "; __cpachecker_tmp_i++){"
                    + (x.split("&")[0]
                        + "[__cpachecker_tmp_i]"
                        + "=__VERIFIER_nondet_ushort();}"
                        + System.lineSeparator());
            break;
          case "long":
          case "signed long":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp +=
                  x.split("&")[1].split(":")[1]
                      + " "
                      + x.split("&")[0]
                      + "["
                      + x.split("&")[1].split(":")[2]
                      + "]"
                      + ";"
                      + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp +=
                "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                    + x.split("&")[1].split(":")[2]
                    + "; __cpachecker_tmp_i++){"
                    + (x.split("&")[0]
                        + "[__cpachecker_tmp_i]"
                        + "=__VERIFIER_nondet_long();}"
                        + System.lineSeparator());
            break;
          case "unsigned long":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp +=
                  x.split("&")[1].split(":")[1]
                      + " "
                      + x.split("&")[0]
                      + "["
                      + x.split("&")[1].split(":")[2]
                      + "]"
                      + ";"
                      + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp +=
                "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                    + x.split("&")[1].split(":")[2]
                    + "; __cpachecker_tmp_i++){"
                    + (x.split("&")[0]
                        + "[__cpachecker_tmp_i]"
                        + "=__VERIFIER_nondet_ulong();}"
                        + System.lineSeparator());
            break;
          case "long double":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp +=
                  x.split("&")[1].split(":")[1]
                      + " "
                      + x.split("&")[0]
                      + "["
                      + x.split("&")[1].split(":")[2]
                      + "]"
                      + ";"
                      + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp +=
                "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                    + x.split("&")[1].split(":")[2]
                    + "; __cpachecker_tmp_i++){"
                    + (x.split("&")[0]
                        + "[__cpachecker_tmp_i]"
                        + "=__VERIFIER_nondet_long_double();}"
                        + System.lineSeparator());
            break;
          case "double":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp +=
                  x.split("&")[1].split(":")[1]
                      + " "
                      + x.split("&")[0]
                      + "["
                      + x.split("&")[1].split(":")[2]
                      + "]"
                      + ";"
                      + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp +=
                "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                    + x.split("&")[1].split(":")[2]
                    + "; __cpachecker_tmp_i++){"
                    + (x.split("&")[0]
                        + "[__cpachecker_tmp_i]"
                        + "=__VERIFIER_nondet_double();}"
                        + System.lineSeparator());
            break;
          case "float":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp +=
                  x.split("&")[1].split(":")[1]
                      + " "
                      + x.split("&")[0]
                      + "["
                      + x.split("&")[1].split(":")[2]
                      + "]"
                      + ";"
                      + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp +=
                "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                    + x.split("&")[1].split(":")[2]
                    + "; __cpachecker_tmp_i++){"
                    + (x.split("&")[0]
                        + "[__cpachecker_tmp_i]"
                        + "=__VERIFIER_nondet_float();}"
                        + System.lineSeparator());
            break;
        }
      } else {
        switch (x.split("&")[1]) {
          case "int":
          case "signed int":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp += x.split("&")[1] + " " + x.split("&")[0] + ";" + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp += (x.split("&")[0] + "=__VERIFIER_nondet_int();" + System.lineSeparator());
            break;
          case "unsigned int":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp += x.split("&")[1] + " " + x.split("&")[0] + ";" + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp += (x.split("&")[0] + "=__VERIFIER_nondet_uint();" + System.lineSeparator());
            break;
          case "char":
          case "signed char":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp += x.split("&")[1] + " " + x.split("&")[0] + ";" + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp += (x.split("&")[0] + "=__VERIFIER_nondet_char();" + System.lineSeparator());
            break;
          case "unsigned char":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp += x.split("&")[1] + " " + x.split("&")[0] + ";" + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp += (x.split("&")[0] + "=__VERIFIER_nondet_uchar();" + System.lineSeparator());
            break;
          case "short":
          case "signed short":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp += x.split("&")[1] + " " + x.split("&")[0] + ";" + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp += (x.split("&")[0] + "=__VERIFIER_nondet_short();" + System.lineSeparator());
            break;
          case "unsigned short":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp += x.split("&")[1] + " " + x.split("&")[0] + ";" + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp += (x.split("&")[0] + "=__VERIFIER_nondet_ushort();" + System.lineSeparator());
            break;
          case "long":
          case "signed long":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp += x.split("&")[1] + " " + x.split("&")[0] + ";" + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp += (x.split("&")[0] + "=__VERIFIER_nondet_long();" + System.lineSeparator());
            break;
          case "unsigned long":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp += x.split("&")[1] + " " + x.split("&")[0] + ";" + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp += (x.split("&")[0] + "=__VERIFIER_nondet_ulong();" + System.lineSeparator());
            break;
          case "long double":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp += x.split("&")[1] + " " + x.split("&")[0] + ";" + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp += (x.split("&")[0] + "=__VERIFIER_nondet_long_double();" + System.lineSeparator());
            break;
          case "double":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp += x.split("&")[1] + " " + x.split("&")[0] + ";" + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp += (x.split("&")[0] + "=__VERIFIER_nondet_double();" + System.lineSeparator());
            break;
          case "float":
            if (Integer.parseInt(x.split("&")[2]) >= lineNumber && !preUsedVariables.contains(x)) {
              tmp += x.split("&")[1] + " " + x.split("&")[0] + ";" + System.lineSeparator();
              preUsedVariables.add(x);
            }
            tmp += (x.split("&")[0] + "=__VERIFIER_nondet_float();" + System.lineSeparator());
            break;
        }
      }
    }
    return tmp;
  }

  private String variablesAlreadyUsed(List<String> preUsedVariables, String line) {
    boolean uVFlag = false;
    String thisLine = line;
    for (String s : preUsedVariables) {
      if (!s.isEmpty() && s.contains("Array")) {
        // zweites line.contains kann zu problemen führen wenn der datentyp teilwort des
        // namens ist
        if (thisLine != null
            && thisLine.contains((s.split("&")[0] + "["))
            && thisLine.contains(s.split("&")[1].split(":")[1])) {
          String tmpArray = thisLine.split("=")[1];
          thisLine =
              s.split("&")[1].split(":")[1]
                  + " __cpachecker_tmp_array["
                  + s.split("&")[1].split(":")[2]
                  + "] = "
                  + tmpArray;
          thisLine = thisLine + " " + s.split("&")[0] + " = __cpachecker_tmp_array;";
        }
      } else {
        if (thisLine != null
            && !s.isEmpty()
            && thisLine.contains(s.split("&")[1])
            && thisLine.contains(";")
            && thisLine.contains(s.split("&")[0])) {
          thisLine = thisLine.split(s.split("&")[1])[1];
          uVFlag = true;
        }
        if (thisLine != null
            && !s.isEmpty()
            && (thisLine.startsWith(" ") || thisLine.startsWith(""))
            && thisLine.endsWith(s.split("&")[0] + ";")
            && uVFlag) {
          thisLine = s.split("&")[0] + "=" + s.split("&")[0] + ";";
        }
      }
    }
    return thisLine;
  }

  private String whileCondition(LoopData loopD, String abstractionLevel) {
    if (abstractionLevel.equals("naiv")) {
      return "if(" + loopD.getCondition() + "){" + System.lineSeparator();
    } else {
    return "for(int cpachecker_i=0; cpachecker_i <"
        + min(
            loopD.getAmountOfPaths(),
            loopD
                .getNumberOutputs())
        + "&&("
        + loopD.getCondition()
        + "); cpachecker_i++"
        + "){"
        + System.lineSeparator();
  }
  }

  private String
      forCondition(LoopData loopD, List<String> preUsedVariables, String abstractionLevel) {
    boolean flag = true;
    String variable = "";
    for (String x : preUsedVariables) {
      if (loopD.getCondition().split(";")[0].contains(x.split("&")[0])) {
        flag = false;
        variable = x.split("&")[1];
      }
    }
    if (abstractionLevel.equals("naiv")) {
      if (flag) {
        return loopD.getCondition().split(";")[0]
            + ";"
            + System.lineSeparator()
            + "if("
            + loopD.getCondition().split(";")[1]
            + "){"
            + System.lineSeparator();
      } else {
        String cond = loopD.getCondition().split(";")[0];

        if (cond.contains(variable)) {
          cond = cond.split(variable)[1];
        }

        return cond
            + ";"
            + System.lineSeparator()
            + "if("
            + loopD.getCondition().split(";")[1]
            + "){"
            + System.lineSeparator();
      }
    } else {
    if (flag) {
    return loopD.getCondition().split(";")[0]
        + ";"
        + System.lineSeparator()
        + "for(int cpachecker_i=0; cpachecker_i <"
        + min(
            loopD.getAmountOfPaths(),
            loopD
                .getNumberOutputs())
        + "&&("
        + loopD.getCondition().split(";")[1]
        + "); cpachecker_i++"
        + "){"
        + System.lineSeparator();
  } else {
    String cond = loopD.getCondition().split(";")[0];

    if (cond.contains(variable)) {
      cond = cond.split(variable)[1];
    }

    return cond
        + ";"
        + System.lineSeparator()
        + "for(int cpachecker_i=0; cpachecker_i <"
        + min(
            loopD.getAmountOfPaths(),
            loopD
                .getNumberOutputs())
        + "&&("
        + loopD.getCondition().split(";")[1]
        + "); cpachecker_i++"
        + "){"
        + System.lineSeparator();
  }
}
  }

  private boolean ifCaseClosed(String line, boolean closed) {

    boolean ifCaseC = closed;

    if (line != null) {
      String temp = line.split("\\(")[0];
      if (!ifCaseC && line.contains("}")) {
        ifCaseC = true;
      }
      if (temp.contains("if") || line.contains("else")) {
      ifCaseC = false;
      if (temp.contains("if")) {
        String temp2 = line.split("if")[1];
        if (temp2.contains("}")) {
          ifCaseC = true;
        }
      } else if (line.contains("else")) {
        String temp2 = line.split("else")[1];
        if (temp2.contains("}")) {
          ifCaseC = true;
        }
      }
    }
  }
    return ifCaseC;
  }

  private void printFile(
      LoopInformation loopInfo,
      String content,
      String pathForNewFile,
      LogManager logger,
      boolean automate) {

    String fileName;

    if (automate) {
      fileName = pathForNewFile;
    } else {
      fileName =
          pathForNewFile + "abstract" + loopInfo.getCFA().getFileNames().get(0).getFileName();
    }

    File file =
        new File(fileName);

    file.getParentFile().mkdirs();
    try (FileWriter fileWriter =
        new FileWriter(file)) {
      String fileContent = content;
      fileWriter.write(fileContent);
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Something is not working with the file you try to import");
    }
  }

  private CFANode findLastNodeInCondition(LoopData loopData) {
    CFANode temp = null;
    if(!loopData.getNodesInCondition().isEmpty()) {
    temp = loopData.getNodesInCondition().get(0);
    for (CFANode node : loopData.getNodesInCondition()) {
      if (temp.getNodeNumber() > node.getNodeNumber()) {
        temp = node;
      }
    }
    } else {
      temp = loopData.getLoopStart();
    }
    return temp;
  }

  private int min(int x, int y) {
    if (x > y) {
      return y;
    } else {
      return x;
    }
  }
}