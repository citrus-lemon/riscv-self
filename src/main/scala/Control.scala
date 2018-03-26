package rself

import chisel3._
import chisel3.util._

object ACT {
  val ALU = 0.U(3.W) // ALU
  val BRC = 1.U(3.W) // BranchCondition
  val LDS = 2.U(3.W) // Load and Store
  val FEN = 3.U(3.W) // Fence
  val SYS = 4.U(3.W) // System call
  val LUI = 5.U(3.W) // Load Upper Immediate
  val AUI = 6.U(3.W) // Add Upper Immediate to PC
  val JMP = 7.U(3.W) // Jump Directly
}

import IMM_MODE._

object Control {
  val opdefault = List(IMM_Z, ACT.SYS)
  val opsel = Array(
    BitPat("b01100") -> List(IMM_R, ACT.ALU),
    BitPat("b00100") -> List(IMM_I, ACT.ALU),
    BitPat("b01000") -> List(IMM_S, ACT.LDS),
    BitPat("b00000") -> List(IMM_I, ACT.LDS),
    BitPat("b11000") -> List(IMM_B, ACT.BRC),
    BitPat("b11100") -> List(IMM_Z, ACT.SYS),
    BitPat("b00011") -> List(IMM_Z, ACT.FEN),
    BitPat("b11011") -> List(IMM_J, ACT.JMP),
    BitPat("b11001") -> List(IMM_I, ACT.JMP),
    BitPat("b00101") -> List(IMM_U, ACT.AUI),
    BitPat("b11011") -> List(IMM_U, ACT.LUI))
}

class Control extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val mode = Output(UInt(4.W))
    val sel  = Output(UInt(3.W))
    val imm  = Output(UInt(3.W))
  })
  val ctrlSignals = ListLookup(io.inst(6, 2), Control.opdefault, Control.opsel)
  io.imm := ctrlSignals(0)
  io.sel := ctrlSignals(1)
  io.mode := MuxLookup(ctrlSignals(1), 0.U(4.W), Seq(
    ACT.ALU -> Mux(io.inst(5) === 1.U,
      Cat(io.inst(30), io.inst(14, 12)),
      Mux(io.inst(13, 12) === 1.U(2.W),
        Cat(io.inst(30), io.inst(14, 12)),
        Cat(0.U(1.W), io.inst(14, 12)))),
    ACT.LDS -> Cat(io.inst(5), io.inst(14, 12)),
    ACT.BRC -> Cat(0.U(1.W), io.inst(14, 12)),
    ACT.JMP -> Cat(0.U(3.W), io.inst(3))))
}
