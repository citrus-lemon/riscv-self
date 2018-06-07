package rself

import chisel3._
import chisel3.util._

class ExecuteIO extends Bundle {
  val en    = Input(Bool())
  val pc    = Input(UInt(32.W))
  val ready = Output(Bool())

  val inst  = Input(UInt(32.W))

  val jmp   = Output(Bool())
  val npc   = Output(UInt(32.W))
  val rd    = Output(UInt(5.W))
  val data  = Output(UInt(32.W))
  val sys   = Output(Bool())

  val mem = Flipped(new MemoryIO)
  val reg = Flipped(new RegFileReaderIO)
}

class Execute extends Module {
  val io = IO(new ExecuteIO)

  val ctrl = Module(new Control)
  val immg = Module(new ImmGen)

  // Components
  val alu = Module(new ALU)
  val brc = Module(new Branch)
  val jmp = Module(new Jump)
  val lds = Module(new LoadStore)
  val aui = Module(new AddUpperImmPC)
  val lui = Module(new LoadUpperImm)
  val fen = Module(new MemoryOperation)
  val sys = Module(new SystemOperation)

  ctrl.io.inst := io.inst

  io.reg.raddr1 := io.inst(19, 15)
  io.reg.raddr2 := io.inst(24, 20)

  immg.io.inst := io.inst
  immg.io.mode := ctrl.io.imm

  val rs1 = io.reg.rdata1
  val rs2 = io.reg.rdata2
  val imm = immg.io.value
  val mode = ctrl.io.mode

  alu.io.A := rs1
  alu.io.B := Mux(ctrl.io.imm === IMM_MODE.IMM_R,
    rs2,
    Utility.extendSign(imm(11, 0)))
  alu.io.alu_op := mode

  brc.io.cond.rs1     := rs1
  brc.io.cond.rs2     := rs2
  brc.io.cond.br_type := mode(2, 0)
  brc.io.jump.disp    := imm(12, 0)
  brc.io.jump.PC      := io.pc

  jmp.io.mode := mode(0)
  jmp.io.pc   := io.pc
  jmp.io.base := rs1
  jmp.io.imm  := imm

  lds.io.inst := io.inst
  lds.io.mode := mode
  lds.io.en   := io.en & (ctrl.io.sel === ACT.LDS)
  lds.io.rs3  := imm
  lds.io.rs2  := rs2
  lds.io.rs1  := rs1
  lds.io.mem  <> io.mem

  aui.io.pc  := io.pc
  aui.io.imm := imm

  lui.io.imm := imm

  fen.io.inst := io.inst

  sys.io.inst := io.inst

  io.data := MuxLookup(ctrl.io.sel, 0.U(32.W), Seq(
    ACT.ALU -> alu.io.out,
    ACT.LDS -> lds.io.ret,
    ACT.LUI -> lui.io.ret,
    ACT.AUI -> aui.io.ret,
    ACT.JMP -> jmp.io.ret))

  io.rd := MuxLookup(ctrl.io.sel, io.inst(11, 7), Seq(
    ACT.BRC -> 0.U(5.W),
    ACT.LDS -> lds.io.rd))

  io.npc := MuxLookup(ctrl.io.sel, 0.U(32.W), Seq(
    ACT.BRC -> brc.io.jmpAddr,
    ACT.JMP -> jmp.io.jaddr,
    ACT.AUI -> aui.io.ret))

  io.jmp := MuxLookup(ctrl.io.sel, false.B, Seq(
    ACT.BRC -> brc.io.isJmp,
    ACT.JMP -> true.B,
    ACT.AUI -> true.B))

  io.sys := ctrl.io.sel === ACT.SYS

  io.ready := MuxLookup(ctrl.io.sel, io.en, Seq(
    ACT.LDS -> lds.io.ready))

}
