package rself

import chisel3._
import chisel3.util._
import chisel3.testers._
import org.scalatest.FlatSpec

class ControlTester(ctl: => Control) extends BasicTester with TestUtils {
  val tester = Module(ctl)
  val (ctl_inst, ctl_mode, ctl_sel, ctl_imm) = randomInsts()
  ctl_inst += 127; ctl_mode += 0; ctl_sel += 4; ctl_imm += 6 // invaild instruction

  shuffle(ctl_inst, ctl_mode, ctl_sel, ctl_imm)
  ctl_inst += 127; ctl_mode += 0; ctl_sel += 4; ctl_imm += 6 // unreachable test

  val inst = interface(ctl_inst)
  val mod  = interface(ctl_mode, 4)
  val sel  = interface(ctl_sel, 3)
  val imm  = interface(ctl_imm, 3)
  val (cntr, done) = Counter(true.B, inst.size)
  tester.io.inst := inst(cntr)

  when(done) { stop(); stop() }
  printf("Counter: %d, Inst: %b, mod: %x ?= %x, sel: %x ?= %x, imm: %x ?= %x\n",
    cntr, tester.io.inst, tester.io.mode, mod(cntr), tester.io.sel, sel(cntr), tester.io.imm, imm(cntr))
  assert(tester.io.mode === mod(cntr))
  assert(tester.io.sel  === sel(cntr))
  assert(tester.io.imm  === imm(cntr))
}

class ControlTests extends FlatSpec {
  "Control" should "pass" in {
    assert(TesterDriver execute (() => new ControlTester(new Control)))
  }
}
