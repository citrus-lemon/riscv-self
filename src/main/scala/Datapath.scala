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
  val npc = RegInit(Const.PC_START.U(32.W))
  val inst = RegInit(Instruction.NOP)

  val vaild = RegInit(false.B)

  val posedgeState = RegInit(false.B)
  io.mem.ren := io.en | posedgeState
  posedgeState := (io.en | posedgeState) & ~io.mem.vaild

  when (reset.toBool) {
    pc := Const.PC_START.U(32.W)
  } .elsewhen (io.en) {
    pc := Mux(io.isJmp, io.jaddr, npc)
    vaild := false.B
  }

  when (io.mem.vaild & ~vaild) {
    inst := io.mem.data
    io.ready := true.B
    npc := pc + 4.U
    vaild := true.B
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

  val inrEnable = RegInit(true.B)
  val exeEnable = RegInit(false.B)
  val wrbEnable = RegInit(false.B)

  val mods = Array(
    inr.io.en -> inr.io.ready,
    exe.io.en -> exe.io.ready,
    wrb.io.en -> wrb.io.ready
  )

  val enReg = mods.indices map { i => RegInit((i == 0).B) }
  val rdReg = mods.indices map { i => RegInit(false.B) }

  val modsize = mods.size

  mods.zipWithIndex foreach { module =>
    val ((en, ready), index) = module
    en := enReg(index)
    rdReg(index) := ready
    enReg((index+1) % modsize) := ready & ~rdReg(index)
  }
}
