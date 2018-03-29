package rself

import chisel3._

class RegFileReaderIO extends Bundle {
  val raddr1 = Input(UInt(5.W))
  val raddr2 = Input(UInt(5.W))
  val rdata1 = Output(UInt(32.W))
  val rdata2 = Output(UInt(32.W))
}

class RegFileWriterIO extends Bundle {
  val wen    = Input(Bool())
  val waddr  = Input(UInt(5.W))
  val wdata  = Input(UInt(32.W))
}

class RegFileIO extends Bundle {
  val reader = new RegFileReaderIO
  val writer = new RegFileWriterIO
}

class RegFile extends Module {
  val io = IO(new RegFileIO)
  val regs = Mem(32, UInt(32.W))
  io.reader.rdata1 := Mux(io.reader.raddr1.orR, regs(io.reader.raddr1), 0.U)
  io.reader.rdata2 := Mux(io.reader.raddr2.orR, regs(io.reader.raddr2), 0.U)
  when(io.writer.wen & io.writer.waddr.orR) {
    regs(io.writer.waddr) := io.writer.wdata
  }
}
