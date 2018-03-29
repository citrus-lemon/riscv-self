package rself

import chisel3._
import chisel3.util._

class MemoryOperation extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
  })
  // Execute no Operation
}

class SystemOperation extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
  })
  // Execute no Operation
}
