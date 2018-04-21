package rself

import chisel3._
import chisel3.util._
import chisel3.testers._
import org.scalatest.FlatSpec

import scala.io.Source
import scala.collection.mutable.ListBuffer

object TestBinary {
  def apply(name: String, bin: scala.collection.Iterable[Long], result: Long) = new TestBinary(name, bin.to[Seq], result)
  def apply(name: String, bin: scala.collection.Iterable[Long]) = {
    val newbin = bin.to[Seq] ++ Seq()
    new TestBinary(name, newbin, 0L)
  }
}

class TestBinary(name: String, bin: Seq[Long], result: Long) {
  def name(): String = name
  def vec() = {
    val newbin = bin ++ Seq(
      Constants.Opcode("SYSTEM")
    )
    VecInit(newbin.map(_.U(32.W)))
  }
  def res() = result.U(32.W)
  def size() = bin.size + 1
}

class DatapathTester(dp: => Datapath, bin: TestBinary) extends BasicTester with TestUtils {
  val tester = Module(dp)
  val mem = Mem(1 << 24, UInt(8.W))
  val runState = RegInit(false.B)

  tester.io.dmem.writer.vaild := runState

  val daddr = tester.io.dmem.reader.addr
  tester.io.dmem.reader.data  := RegNext(Cat(mem(daddr+3.U), mem(daddr+2.U), mem(daddr+1.U), mem(daddr)))
  tester.io.dmem.reader.vaild := runState
  val iaddr = tester.io.imem.addr
  tester.io.imem.data  := RegNext(Cat(mem(iaddr+3.U), mem(iaddr+2.U), mem(iaddr+1.U), mem(iaddr)))
  tester.io.imem.vaild := runState

  val insts = bin.vec()
  val result = bin.res()
  val (cntr, done) = Counter(~runState, bin.size)

  val stopper = RegInit(0.U(16.W))
  stopper := stopper + 1.U
  when (stopper > 0x8000.U) { stop(); stop() }

  when (~runState) {
    // memory ready
    val data = insts(cntr)
    val wiaddr = (cntr << 2.U) + Const.PC_START.U(32.W)
    mem(wiaddr      ) := data(7, 0)
    mem(wiaddr + 1.U) := data(15, 8)
    mem(wiaddr + 2.U) := data(23, 16)
    mem(wiaddr + 3.U) := data(31, 24)
    when(done) { runState := true.B }
  } .otherwise {
    when (tester.io.imem.ren) {
      printf("INST[%x] ==> %x, iaddr: %x\n", iaddr, Cat(mem(iaddr+3.U), mem(iaddr+2.U), mem(iaddr+1.U), mem(iaddr)), iaddr)
    }
    when (tester.io.dmem.writer.wen) {
      // memory write
      val waddr = tester.io.dmem.writer.addr
      val wdata = tester.io.dmem.writer.data
      switch(tester.io.dmem.writer.mode) {
        is (0.U) {
          mem(waddr) := wdata(7, 0)
          printf("MEM[%x] <== BYTE  %x\n", waddr, wdata(7,0))
        }
        is (1.U) {
          mem(waddr      ) := wdata(7, 0)
          mem(waddr + 1.U) := wdata(15, 8)
          printf("MEM[%x] <== HWORD %x\n", waddr, wdata(15,0))
        }
        is (2.U) {
          mem(waddr      ) := wdata(7, 0)
          mem(waddr + 1.U) := wdata(15, 8)
          mem(waddr + 2.U) := wdata(23, 16)
          mem(waddr + 3.U) := wdata(31, 24)
          printf("MEM[%x] <== WORD  %x\n", waddr, wdata)
        }
        is (3.U) {
          mem(waddr      ) := wdata(7, 0)
          mem(waddr + 1.U) := wdata(15, 8)
          mem(waddr + 2.U) := wdata(23, 16)
          mem(waddr + 3.U) := wdata(31, 24)
          printf("MEM[%x] <== WORD  %x\n", waddr, wdata)
        }
      }
    }
    when (tester.io.dmem.reader.ren) {
      printf("MEM[%x] ==> %x\n", daddr, Cat(mem(daddr+3.U), mem(daddr+2.U), mem(daddr+1.U), mem(daddr)))
    }
    when(tester.io.sys) {
      // assert
      stop(); stop()
    }
  }
}

object testAss {
  val a = TestBinary("Hello", Seq(
    19L, 19L, 19L
  ))
}

class DatapathTests extends FlatSpec {
  "DataPath" should "pass" in {
    assert(TesterDriver execute (() => new DatapathTester(new Datapath, testAss.a)))
  }
}
