package rself

import chisel3._
import chisel3.util._

class MemoryReaderIO extends Bundle {
  val addr = Input(UInt(32.W))
  val ren  = Input(Bool())
  val data = Output(UInt(32.W))
  val vaild = Output(Bool())
}

class MemoryWriterIO extends Bundle {
  val addr = Input(UInt(32.W))
  val mode = Input(UInt(2.W))
  val wen  = Input(Bool())
  val data = Input(UInt(32.W))
  val vaild = Output(Bool())
}

class MemoryIO extends Bundle {
  val writer = new MemoryWriterIO
  val reader = new MemoryReaderIO
}
