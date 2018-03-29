package rself

import chisel3._
import chisel3.util._

class Jump extends Module {
  val io = IO(new Bundle {
    val mode = Input(Bool())
    val pc   = Input(UInt(32.W))
    val imm  = Input(UInt(32.W))
    val base = Input(UInt(32.W))
    val ret   = Output(UInt(32.W))
    val jaddr = Output(UInt(32.W))
  })
  def s(i: Int): UInt = Utility.extendSign(io.imm(i, 0), 32)
  io.ret   := io.pc + 4.U(32.W)
  io.jaddr := Mux(io.mode, s(20), s(11)) + Mux(io.mode, io.pc, io.base)
}

class AddUpperImmPC extends Module {
  val io = IO(new Bundle {
    val pc   = Input(UInt(32.W))
    val imm  = Input(UInt(32.W))
    val ret = Output(UInt(32.W))
  })
  io.ret := io.imm + io.pc
}

class LoadUpperImm extends Module {
  val io = IO(new Bundle {
    val imm  = Input(UInt(32.W))
    val ret = Output(UInt(32.W))
  })
  io.ret := io.imm
}
