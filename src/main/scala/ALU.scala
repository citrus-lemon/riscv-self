package rself

import chisel3._
import chisel3.util._

import ALU_MODE._

class ALUIO extends Bundle {
  val A = Input(UInt(32.W))
  val B = Input(UInt(32.W))
  val alu_op = Input(UInt(4.W))
  val out = Output(UInt(32.W))
}

class ALU extends Module {
  val io = IO(new ALUIO)
  val shamt = io.B(4,0).asUInt

  io.out := MuxLookup(io.alu_op, io.B, Seq(
    ALU_ADD  -> (io.A + io.B),
    ALU_SUB  -> (io.A - io.B),
    ALU_SRA  -> (io.A.asSInt >> shamt).asUInt,
    ALU_SRL  -> (io.A >> shamt),
    ALU_SLL  -> (io.A << shamt),
    ALU_SLT  -> (io.A.asSInt < io.B.asSInt),
    ALU_SLTU -> (io.A < io.B),
    ALU_AND  -> (io.A & io.B),
    ALU_OR   -> (io.A | io.B),
    ALU_XOR  -> (io.A ^ io.B)))
}
