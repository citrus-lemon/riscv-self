package rself

import chisel3._
import chisel3.util._

object Utility {
  def extendSign(m: UInt, b: Int = 32): UInt = {
    val extendWire = Wire(SInt(b.W))
    extendWire := m.asSInt
    extendWire.asUInt
  }
}

class Posedge extends Module {
  val io = IO(new Bundle {
    val in  = Input(Bool())
    val re  = Input(Bool())
    val out = Output(Bool())
  })
  val state = RegInit(false.B)
  io.out := (io.in | state) & io.re
  state := io.out
}
