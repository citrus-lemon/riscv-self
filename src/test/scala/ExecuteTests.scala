package rself

import chisel3._
import chisel3.util._
import chisel3.testers._
import org.scalatest.FlatSpec

trait ExecuteUtils extends TestUtils {
  val regData = (0 until 32).map((i) => if (i != 0) random32() else 0L)
  def instResults(cs: List4): List4 = {
    val (inst, mode, sel, imm) = cs
    val (data, rd, npc, jmp) = TestList.List4()
    val size = inst.size
    import Constants._
    val Act = ControlActs.map(_.swap)
    val Imm = ImmModes.map(_.swap)
    for (i <- 0 until size) {
      def B(hi: Int, lo: Int = -1) = {
        val low = if (lo == -1) {hi} else {lo}
        (inst(i) & ((1<<(hi+1)) - (1<<low))) >> low
      }
      val rs3 = immData(inst(i), Imm(imm(i)))
      val rs2 = regData(B(24, 20).toInt)
      val rs1 = regData(B(19, 15).toInt)
      val rd0 = B(11, 7)
      // TODO: make assertion results
    }
    return (data, rd, npc, jmp)
  }
}

class ExecuteTester(exe: => Execute) extends BasicTester with ExecuteUtils {
  val tester = Module(exe)
  val (ctl_inst, ctl_mode, ctl_sel, ctl_imm) = randomInsts() // TestUtils::randomInsts
  ctl_inst += 127; ctl_mode += 0; ctl_sel += 4; ctl_imm += 6 // invaild instruction

  val inst = interface(ctl_inst)
  val regs = interface(regData)
  val pcReg = RegInit(Const.PC_START.U(32.W))
  pcReg := pcReg + 1.U
  val (cntr, done) = Counter(true.B, inst.size)
  tester.io.en := true.B
  tester.io.pc := pcReg
  tester.io.inst := inst(cntr)
  tester.io.reg.rdata1 := regs(tester.io.reg.raddr1)
  tester.io.reg.rdata2 := regs(tester.io.reg.raddr2)
  tester.io.mem.writer.vaild := true.B
  tester.io.mem.reader.vaild := true.B
  tester.io.mem.reader.data := 0.U

  when(done) { stop(); stop() }
  printf("Counter: %d, Inst: %b, isJump: %b, nextPC: %x, rd: %d, data: %x, sys:%b\n",
    cntr, tester.io.inst, tester.io.jmp, tester.io.npc, tester.io.rd, tester.io.data, tester.io.sys)
  // TODO: no assertion
}

class ExecuteTests extends FlatSpec {
  "Execute" should "pass" in {
    assert(TesterDriver execute (() => new ExecuteTester(new Execute)))
  }
}
