package rself

import chisel3._
import chisel3.util._

class LoadIO extends Bundle {
  val mode = Input(UInt(3.W))
  val imm  = Input(UInt(12.W))
  val rs1  = Input(UInt(32.W))
  val en   = Input(Bool())
  val ready = Output(Bool())
  val data = Output(UInt(32.W))
  val mem = Flipped(new MemoryReaderIO)
}

class StoreIO extends Bundle {
  val mode = Input(UInt(3.W))
  val imm  = Input(UInt(12.W))
  val rs2  = Input(UInt(32.W))
  val rs1  = Input(UInt(32.W))
  val en   = Input(Bool())
  val ready = Output(Bool())
  val mem = Flipped(new MemoryWriterIO)
}

class Load extends Module {
  val io = IO(new LoadIO)
  val enable = Module(new Posedge)
  enable.io.in := io.en
  enable.io.re := ~io.mem.vaild
  io.mem.addr := io.rs1 + Utility.extendSign(io.imm)
  io.mem.ren := enable.io.out
  io.ready := io.mem.vaild

  def UNS(i: Int):UInt = Mux(~io.mode(2), Utility.extendSign(io.mem.data(i-1, 0), 32), io.mem.data(i-1, 0))

  io.data := MuxLookup(io.mode(1, 0), 0.U(32.W), Seq(
    0.U(2.W) -> UNS(8),
    1.U(2.W) -> UNS(16),
    2.U(2.W) -> UNS(32),
    3.U(2.W) -> UNS(32))) // 32bits core treat 64bits insts as 32bits
}

class Store extends Module {
  val io = IO(new StoreIO)
  val enable = Module(new Posedge)
  enable.io.in := io.en
  enable.io.re := ~io.mem.vaild
  io.mem.addr := io.rs1 + Utility.extendSign(io.imm)
  io.mem.wen := enable.io.out
  io.mem.data := io.rs2
  io.mem.mode := io.mode
  io.ready := io.mem.vaild
}

class LoadStore extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val mode = Input(UInt(4.W))
    val en   = Input(Bool())
    val rs3 = Input(UInt(32.W))
    val rs2 = Input(UInt(32.W))
    val rs1 = Input(UInt(32.W))
    val ret = Output(UInt(32.W))
    val rd = Output(UInt(5.W))
    val ready = Output(Bool())

    val mem = Flipped(new MemoryIO)
  })
  val load  = Module(new Load)
  val store = Module(new Store)
  io.rd := Mux(io.mode(3), 0.U(5.W), io.inst(11, 7))

  load.io.imm := io.rs3(11, 0)
  load.io.rs1 := io.rs1

  store.io.imm := io.rs3(11, 0)
  store.io.rs2 := io.rs2
  store.io.rs1 := io.rs1

  load.io.en  := (~io.mode(3)) & io.en
  store.io.en := ( io.mode(3)) & io.en

  load.io.mode := io.mode(2, 0)
  store.io.mode := io.mode(2, 0)

  io.mem.reader <> load.io.mem
  io.mem.writer <> store.io.mem

  io.ret := load.io.data
  io.ready := Mux(io.mode(3), store.io.ready, load.io.ready)
}
