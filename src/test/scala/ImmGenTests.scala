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
    def B(hi: Int, lo: Int = -1) = {
      val low = if (lo == -1) {hi} else {lo}
      (inst & ((1<<(hi+1)) - (1<<low))) >> low
    }
    img_inst += inst
    img_mode += modop
    img_val += (modstr match {
      // operator `<<' higher than `^'
      case 'R' => 0
      case 'I' => B(31, 20)
      case 'S' => B(31, 25)<<5 ^ B(11, 7)
      case 'B' => B(31)<<12 ^ B(7)<<11 ^ B(30, 25)<<5 ^ B(11, 8)<<1
      case 'J' => B(31)<<20 ^ B(19, 12)<<12 ^ B(20)<<11 ^ B(30, 21)<<1
      case 'U' => B(31, 12)<<12
      case  _  => 0
    })
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
