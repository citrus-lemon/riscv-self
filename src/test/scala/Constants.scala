// auto generate by python scripts

package rself

import chisel3._

object Constants {
  val ALU = Map(
    "ADD"  -> 0L,
    "SUB"  -> 8L,
    "SLL"  -> 1L,
    "SLT"  -> 2L,
    "SLTU" -> 3L,
    "XOR"  -> 4L,
    "SRL"  -> 5L,
    "SRA"  -> 13L,
    "OR"   -> 6L,
    "AND"  -> 7L)
  val Opcode = Map(
    "LUI"    -> 55L,
    "AUIPC"  -> 23L,
    "JAL"    -> 111L,
    "JALR"   -> 103L,
    "BRANCH" -> 99L,
    "LOAD"   -> 3L,
    "STORE"  -> 35L,
    "ITYPE"  -> 19L,
    "RTYPE"  -> 51L,
    "MEMORY" -> 15L,
    "SYSTEM" -> 115L)
  val BranchCodes = Map(
    "BEQ"  -> 0L,
    "BNE"  -> 1L,
    "BLT"  -> 4L,
    "BGE"  -> 5L,
    "BLTU" -> 6L,
    "BGEU" -> 7L)
  val LoadCodes = Map(
    "LB"  -> 0L,
    "LH"  -> 1L,
    "LW"  -> 2L,
    "LD"  -> 3L,
    "LBU" -> 4L,
    "LHU" -> 5L,
    "LWU" -> 6L)
  val StoreCodes = Map(
    "SB" -> 0L,
    "SH" -> 1L,
    "SW" -> 2L,
    "SD" -> 3L)
  val ControlActs = Map(
    "ALU" -> 0L,
    "BRC" -> 1L,
    "LDS" -> 2L,
    "FEN" -> 3L,
    "SYS" -> 4L,
    "LUI" -> 5L,
    "AUI" -> 6L,
    "JMP" -> 7L)
  val ImmModes = Map(
    "R" -> 0L,
    "I" -> 1L,
    "S" -> 2L,
    "U" -> 3L,
    "J" -> 4L,
    "B" -> 5L,
    "Z" -> 6L)
}

