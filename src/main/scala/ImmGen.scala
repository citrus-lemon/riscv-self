package rself

import chisel3._
import chisel3.util._

import IMM_MODE._

class ImmGenIO extends Bundle {
  val inst  = Input(UInt(32.W))
  val mode  = Input(UInt(3.W))
  val value = Output(UInt(32.W))
}

class ImmGen extends Module {
  val io = IO(new ImmGenIO)
  io.value := MuxLookup(io.mode, 0.U(32.W), Seq(
    IMM_R -> 0.U(32.W),
    IMM_I -> Cat(0.U(20.W), io.inst(31, 20)),
    IMM_S -> Cat(0.U(20.W), io.inst(31, 25), io.inst(11, 7)),
    IMM_B -> Cat(0.U(19.W), io.inst(31), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W)),
    IMM_J -> Cat(0.U(11.W), io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W)),
    IMM_U -> Cat(io.inst(31, 12), 0.U(12.W))))
}