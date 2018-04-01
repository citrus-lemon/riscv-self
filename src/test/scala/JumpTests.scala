package rself

import chisel3._
import chisel3.util._
import chisel3.testers._
import org.scalatest.FlatSpec

// This test file is for src/main/scala/Jump.scala
//   with modules `Jump' `AddUpperImmPC' and `LoadUpperImm'

class JumpOnlyTester(jmp: => Jump) extends BasicTester with TestUtils {
  val tester = Module(jmp)
  val mode = TestList()
  val pc   = TestList()
  val imm  = TestList()
  val base = TestList()
  val ret  = TestList()
  val addr = TestList()
  for (m <- Array(0, 1); i <- 0 until 10) {
    mode += m
    val (r_pc, r_base) = (random32(), random32())
    val bits = (m match {
      case 0 => 12
      case 1 => 21
    })
    val vaild_imm = randombits(bits).toSigned(bits)
    val redundancy_imm = randombits(32-bits)
    imm += (vaild_imm.toUnsigned(bits) + (redundancy_imm<<bits))
    ret += (r_pc + 4).toUnsigned()
    pc += r_pc; base += r_base
    addr += ((if (m == 1) {r_pc} else {r_base}) + vaild_imm).toUnsigned()
  }
  shuffle(mode, pc, imm, base, ret, addr)
  mode += 0; pc += 0; imm += 0; base += 0; ret += 4; addr += 0 // unreachable test

  val m = interface(mode, 1)
  val p = interface(pc)
  val i = interface(imm)
  val b = interface(base)
  val r = interface(ret)
  val a = interface(addr)
  val (cntr, done) = Counter(true.B, m.size)
  tester.io.mode := m(cntr)
  tester.io.pc   := p(cntr)
  tester.io.imm  := i(cntr)
  tester.io.base := b(cntr)

  when(done) { stop(); stop() }
  printf("Counter: %d, Mode: %b, PC: %x, IMM: %x, RS1: %x, PC+4: %x ?= %x, jmpAddr: %x ?= %x\n",
    cntr, tester.io.mode, tester.io.pc, tester.io.imm, tester.io.base, tester.io.ret, r(cntr), tester.io.jaddr, a(cntr))
  assert(tester.io.ret === r(cntr))
  assert(tester.io.jaddr === a(cntr))
}

class AUIPCTester(aui: => AddUpperImmPC) extends BasicTester with TestUtils {
  val tester = Module(aui)
  val pc  = TestList()
  val imm = TestList()
  val ret = TestList()
  for (i <- 0 until 10) {
    val r = (random32(), random32())
    pc += r._1; imm += r._2; ret += (r._1 + r._2).toUnsigned()
  }
  shuffle(pc, imm, ret)
  pc += 0; imm += 0; ret += 0 // unreachable test

  val p = interface(pc)
  val i = interface(imm)
  val r = interface(ret)
  val (cntr, done) = Counter(true.B, r.size)
  tester.io.pc  := p(cntr)
  tester.io.imm := i(cntr)

  when(done) { stop(); stop() }
  printf("Counter: %d, PC: %x, IMM: %x, ret: %x ?= %x\n",
    cntr, tester.io.pc, tester.io.imm, tester.io.ret, r(cntr))
  assert(tester.io.ret === r(cntr))
}

class LUITester(lui: => LoadUpperImm) extends BasicTester with TestUtils {
  val tester = Module(lui)
  val pc  = TestList()
  val imm = TestList()
  val ret = TestList()
  for (i <- 0 until 10) {
    val r = random32()
    imm += r; ret += r
  }
  shuffle(imm, ret)
  imm += 0; ret += 0 // unreachable test

  val i = interface(imm)
  val r = interface(ret)
  val (cntr, done) = Counter(true.B, r.size)
  tester.io.imm := i(cntr)

  when(done) { stop(); stop() }
  printf("Counter: %d, IMM: %x, ret: %x ?= %x\n",
    cntr, tester.io.imm, tester.io.ret, r(cntr))
  assert(tester.io.ret === r(cntr))
}

class JumpTests extends FlatSpec {
  "Jump" should "pass" in {
    assert(TesterDriver execute (() => new JumpOnlyTester(new Jump)))
  }
  "AUIPC" should "pass" in {
    assert(TesterDriver execute (() => new AUIPCTester(new AddUpperImmPC)))
  }
  "LUI" should "pass" in {
    assert(TesterDriver execute (() => new LUITester(new LoadUpperImm)))
  }
}
