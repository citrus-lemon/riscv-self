package rself

import chisel3._
import chisel3.util._
import chisel3.testers._
import org.scalatest.FlatSpec

// simple implementation for Memory, only for test
class Memory extends Module {
  val io = IO(new MemoryIO)
}

class PosedgeTester(p: => Posedge) extends BasicTester with TestUtils {
  val tester = Module(p)

  val ser = TestList()
  val res = TestList()
  val out = TestList()
  for (i <- 0 until 100) {
    ser += 1
    res += 1
    out += 1
    for (j <- 0L until randombits(3)) {
      ser += 0
      res += 1
      out += 1
    }
    res += 0
    ser += 0
    out += 0
    for (j <- 0L until randombits(2)) {
      ser += 0
      res += 1
      out += 0
    }
  }

  val o = interface(ser, 1)
  val e = interface(res, 1)
  val t = interface(out, 1)
  val (cntr, done) = Counter(true.B, o.size)
  when(done) { printf("\n"); stop(); stop() }
  tester.io.in := o(cntr)
  tester.io.re := e(cntr)
  assert(tester.io.out === t(cntr))
  // printf("%d ", tester.io.out)
}

abstract class LoadStoreTester extends BasicTester with TestUtils {

}

class LoadTester(ls: => LoadStore) extends LoadStoreTester {
  val tester = Module(ls)

  val _time = TestList()
  val _mode = TestList()
  val _imm  = TestList()
  val _rs1  = TestList()
  val _rs2  = TestList()
  val _rd   = TestList()
  val _cal  = TestList()
  val _mem  = TestList()

  for (i <- 0 until 100) {
    val r = (random32(), randombits(12))
    _time += randombits(3)
    _mode += 2
    _rd  += randombits(5)
    _rs2 += 0
    _rs1 += r._1
    _imm += r._2
    _cal += (r._1 + r._2.toSigned(12)).toUnsigned()
    _mem += random32()
  }

  val time = interface(_time, 8)
  val mode = interface(_mode, 4)
  val rs3  = interface(_imm)
  val rs2  = interface(_rs2)
  val rs1  = interface(_rs1)
  val rd   = interface(_rd, 5)
  val inst = interface(_rd map (x => x<<7))

  val mem = interface(_mem)

  val counter     = RegInit(0.U(16.W))
  val timecounter = RegInit(0.U(8.W))

  // tester.io.mem.reader.addr := DontCare
  // tester.io.mem.reader.ren  := DontCare
  // tester.io.mem.writer.addr := DontCare
  // tester.io.mem.writer.mode := DontCare
  // tester.io.mem.writer.wen  := DontCare
  // tester.io.mem.writer.data := DontCare
  tester.io.mem.writer.vaild := false.B

  tester.io.mode := mode(counter)
  tester.io.inst := inst(counter)
  tester.io.rs3  := rs3(counter)
  tester.io.rs2  := rs2(counter)
  tester.io.rs1  := rs1(counter)

  val vaild = RegInit(false.B)
  val rdata = RegInit(0.U(32.W))
  val enabl = RegInit(false.B)
  val nowmem = RegInit(0.U(32.W))

  nowmem := mem(counter)

  tester.io.mem.reader.vaild := vaild
  tester.io.mem.reader.data  := rdata
  tester.io.en := enabl

  val stopper = RegInit(0.U(16.W))
  stopper := stopper + 1.U
  when (stopper > 0x8000.U) { stop(); stop() }

  when (counter >= time.size.U) { stop(); stop() }

  when (tester.io.mem.reader.ren) {
    timecounter := timecounter + 1.U
    printf("Counter: %d, TimeCounter: %d\n", counter, timecounter)
  } .otherwise {
    timecounter := 0.U
  }

  when (timecounter >= time(counter)) {
    vaild := true.B
    rdata  := mem(counter)
    printf("Counter: %d, TimeReturn, Wait: %d ?= %d\n",
      counter, timecounter, time(counter))
    assert(timecounter === time(counter))
    timecounter := 0.U
    counter := counter + 1.U
  } .otherwise {
    vaild := false.B
  }

  when ((timecounter === 0.U) && (time(counter) =/= 0.U)) {
    printf("Counter: %d, Enable, Wait: %d, rs3: %x, rs2: %x, rs1: %x  =>  rd: %d\n",
      counter, time(counter), rs3(counter), rs2(counter), rs1(counter), rd(counter))
    enabl := true.B
  } .elsewhen (time(counter) === 0.U) {
    printf("Counter: %d, Disable\n", counter)
    enabl := false.B
    counter := counter + 1.U
  } .otherwise {
    enabl := false.B
  }

  when (tester.io.ready) {
    printf("Counter: %d, Ready, Data: %x ?= %x\n", counter, tester.io.ret, nowmem)
    assert(nowmem === tester.io.ret)
  }
}

class StoreTester(ls: => LoadStore) extends LoadStoreTester {

}

class LoadStoreTests extends FlatSpec {
  "Posedge" should "pass" in {
    assert(TesterDriver execute (() => new PosedgeTester(new Posedge)))
  }
  "Load" should "pass" in {
    assert(TesterDriver execute (() => new LoadTester(new LoadStore)))
  }
  "Store" should "pass" in {
    // assert(TesterDriver execute (() => new StoreTester(new LoadStore)))
  }
}
