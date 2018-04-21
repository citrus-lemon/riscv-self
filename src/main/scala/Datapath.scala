package rself

import chisel3._
import chisel3.util._

object Const {
  val PC_START = 0x200
  val PC_EVEC  = 0x100
}

object Instruction {
  def NOP = BitPat.bitPatToUInt(BitPat("b00000000000000000000000000010011"))
}

class InstructionReader extends Module {
  val io = IO(new Bundle {
    val en    = Input(Bool())
    val ready = Output(Bool())

    val isJmp = Input(Bool())
    val jaddr = Input(UInt(32.W))

    val inst  = Output(UInt(32.W))
    val pc    = Output(UInt(32.W))

    val mem = Flipped(new MemoryReaderIO)
  })
  val pc  = RegInit(Const.PC_START.U(32.W))
  val npc = pc + 4.U
  val inst = RegInit(Instruction.NOP)
  val enable = Module(new Posedge)

  enable.io.in := io.en
  enable.io.re := !io.mem.vaild
  io.mem.ren := enable.io.out

  when (io.en) {
    pc := Mux(io.isJmp, io.jaddr, npc)
  }

  when (io.mem.vaild) {
    inst := io.mem.data
    io.ready := true.B
  } .otherwise {
    io.ready := false.B
  }

  io.mem.addr := pc
  io.inst := inst
  io.pc := pc
}

class WriteBack extends Module {
  val io = IO(new Bundle {
    val en    = Input(Bool())
    val ready = Output(Bool())

    val rd   = Input(UInt(5.W))
    val data = Input(UInt(32.W))

    val reg = Flipped(new RegFileWriterIO)
  })

  io.reg.waddr := io.rd
  io.reg.wdata := io.data
  io.reg.wen := io.en

  when (io.en) {
    io.ready := true.B
  }.otherwise {
    io.ready := false.B
  }
}

class DatapathIO extends Bundle {
  val dmem = Flipped(new MemoryIO)
  val imem = Flipped(new MemoryReaderIO)
  val sys  = Output(Bool())
}

class Datapath extends Module {
  val io  = IO(new DatapathIO)
  val reg = Module(new RegFile)
  val exe = Module(new Execute)
  val inr = Module(new InstructionReader)
  val wrb = Module(new WriteBack)

  exe.io.inst := inr.io.inst
  exe.io.pc   := inr.io.pc

  inr.io.isJmp := exe.io.jmp
  inr.io.jaddr := exe.io.npc

  wrb.io.rd   := exe.io.rd
  wrb.io.data := exe.io.data

  exe.io.mem <> io.dmem
  inr.io.mem <> io.imem
  exe.io.reg <> reg.io.reader
  wrb.io.reg <> reg.io.writer

  io.sys := exe.io.sys

  when (wrb.io.ready | reset.toBool) {
    inr.io.en := true.B
  }.otherwise {
    inr.io.en := false.B
  }

  when (inr.io.ready) {
    exe.io.en := true.B
  }.otherwise {
    exe.io.en := false.B
  }

  when (exe.io.ready) {
    wrb.io.en := true.B
  }.otherwise {
    wrb.io.en := false.B
  }

}
