package rself

import chisel3._
import chisel3.util._
import chisel3.testers._
import org.scalatest.FlatSpec

class BranchTester(brc: => Branch) extends BasicTester with TestUtils {
  val tester = Module(brc)
  val rs1  = TestList()
  val rs2  = TestList()
  val jmty = TestList()
  val disp = TestList()
  val pc   = TestList()
  val isJm = TestList()
  val addr = TestList()

  for (func <- Constants.BranchCodes; i <- 0 until 10) {
    val (fn, fid) = func
    jmty += fid
    val rrs = (fn match {
      case "BEQ" | "BNE" => {
        val a = random32()
        if (rnd.nextBoolean()) {
          (a, a)
        } else {
          (a, random32())
        }
      }
      case _ => (random32(), random32())
    })
    rs1 += rrs._1; rs2 += rrs._2

    val rdisp = randombits(13)
    disp += rdisp

    val cpc = random32()
    val jpc = (cpc + rdisp.toSigned(13)).toUnsigned()
    pc += cpc; addr += jpc

    isJm += (if (fn match {
      case "BEQ" => rrs._1 == rrs._2
      case "BNE" => rrs._1 != rrs._2
      case "BLT" => rrs._1.toInt < rrs._2.toInt
      case "BGE" => rrs._1.toInt >= rrs._2.toInt
      case "BLTU" => rrs._1 < rrs._2
      case "BGEU" => rrs._1 >= rrs._2
    }) {1L} else {0L})
  }
  shuffle(rs1, rs2, jmty, disp, pc, isJm, addr)
  rs1 += 0; rs2 += 0; jmty += BR_TYPE.BR_EQ.litValue().toLong; disp += 0; pc += 0; isJm += 1; addr += 0 // unreachable test

  val brc_cond_rs1 = interface(rs1)
  val brc_cond_rs2 = interface(rs2)
  val brc_cond_type = interface(jmty, 3)
  val brc_jump_disp = interface(disp, 13)
  val brc_jump_pc   = interface(pc)
  val brc_out_isJmp = interface(isJm, 1)
  val brc_out_jAddr = interface(addr)

  val (cntr, done) = Counter(true.B, brc_jump_pc.size)

  tester.io.cond.rs1 := brc_cond_rs1(cntr)
  tester.io.cond.rs2 := brc_cond_rs2(cntr)
  tester.io.cond.br_type := brc_cond_type(cntr)
  tester.io.jump.disp := brc_jump_disp(cntr)
  tester.io.jump.PC := brc_jump_pc(cntr)

  when(done) { stop(); stop() }
  printf("Counter: %d, Br: %d, A: %x, B: %x, isJump: %b ?= %b, PC: %x, disp: %x, jumpAddr: %x ?= %x\n",
    cntr, tester.io.cond.br_type, tester.io.cond.rs1, tester.io.cond.rs2,
    tester.io.isJmp, brc_out_isJmp(cntr),
    tester.io.jump.PC, tester.io.jump.disp,
    tester.io.jmpAddr, brc_out_jAddr(cntr))
  assert(tester.io.isJmp === brc_out_isJmp(cntr))
  assert(tester.io.jmpAddr === brc_out_jAddr(cntr))
}

class BranchTests extends FlatSpec {
  "Branch" should "pass" in {
    assert(TesterDriver execute (() => new BranchTester(new Branch)))
  }
}
