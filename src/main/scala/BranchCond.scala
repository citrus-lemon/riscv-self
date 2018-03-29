package rself

import chisel3._
import chisel3.util._

import BR_TYPE._

class BrCondIO extends Bundle {
  val rs1 = Input(UInt(32.W))
  val rs2 = Input(UInt(32.W))
  val br_type = Input(UInt(3.W))
}

class BrJumpIO extends Bundle {
  val disp = Input(UInt(13.W))
  val PC = Input(UInt(32.W))
}

class BrCond extends Module {
  val io = IO(new Bundle {
    val cond = new BrCondIO
    val isJmp = Output(Bool())
  })
  val xlen = 32
  val diff = io.cond.rs1 - io.cond.rs2
  val neq  = diff.orR
  val eq   = !neq
  val isSameSign = io.cond.rs1(xlen-1) === io.cond.rs2(xlen-1)
  val lt   = Mux(isSameSign, diff(xlen-1), io.cond.rs1(xlen-1))
  val ltu  = Mux(isSameSign, diff(xlen-1), io.cond.rs2(xlen-1))
  val ge   = !lt
  val geu  = !ltu

  io.isJmp :=
    ((io.cond.br_type === BR_EQ) && eq) ||
    ((io.cond.br_type === BR_NE) && neq) ||
    ((io.cond.br_type === BR_LT) && lt) ||
    ((io.cond.br_type === BR_GE) && ge) ||
    ((io.cond.br_type === BR_LTU) && ltu) ||
    ((io.cond.br_type === BR_GEU) && geu)
}

class BrJump extends Module {
  val io = IO(new Bundle {
    val jmpAddr = Output(UInt(32.W))
    val jmp = new BrJumpIO
  })
  io.jmpAddr := io.jmp.PC + Utility.extendSign(io.jmp.disp, 32)
}

class Branch extends Module {
  val io = IO(new Bundle {
    val cond = new BrCondIO
    val jump = new BrJumpIO
    val jmpAddr = Output(UInt(32.W))
    val isJmp = Output(Bool())
  })
  val modCond = Module(new BrCond)
  val modJump = Module(new BrJump)
  modCond.io.cond := io.cond
  modJump.io.jmp := io.jump

  io.isJmp := modCond.io.isJmp
  io.jmpAddr := modJump.io.jmpAddr
}
