package rself

import chisel3._
import chisel3.util._
import chisel3.testers._
import org.scalatest.FlatSpec

import IMM_MODE._

class ImmGenTester(img: => ImmGen) extends BasicTester with TestUtils {
  val tester = Module(img)
  import scala.collection.mutable.ListBuffer
  val img_inst = TestList()
  val img_mode = TestList()
  val img_val  = TestList()
  for (mod <- Constants.ImmModes; i <- 0 until 15) {
    val (modstr, modop) = mod
    val inst = random32()
    img_inst += inst
    img_mode += modop
    img_val += immData(inst, modstr) // TestUtils::immData
  }

  shuffle(img_inst, img_mode, img_val)
  img_inst += 0; img_mode += IMM_Z.litValue().toLong; img_val += 0 // unreachable test

  val inst  = interface(img_inst)
  val mode  = interface(img_mode, 3)
  val value = interface(img_val)
  val (cntr, done) = Counter(true.B, inst.size)
  tester.io.inst := inst(cntr)
  tester.io.mode := mode(cntr)

  when(done) { stop(); stop() }
  printf("Counter: %d, Inst: %b, mod: %x, value: %x ?= %x\n",
    cntr, tester.io.inst, tester.io.mode, tester.io.value, value(cntr))
  assert(tester.io.value === value(cntr))
}

class ImmGenTests extends FlatSpec {
  "ImmGen" should "pass" in {
    assert(TesterDriver execute (() => new ImmGenTester(new ImmGen)))
  }
}
