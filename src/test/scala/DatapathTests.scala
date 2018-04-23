package rself

import chisel3._
import chisel3.util._
import chisel3.testers._
import org.scalatest.FlatSpec

import scala.io.Source
import scala.collection.mutable.ListBuffer

object PesudoAssemblyTools {
  import Constants._
  def LOAD(c: String, rd: Long, imm: Long, rs1: Long) = IMMTYPE("I", "LOAD", rd, rs1, 0, imm, LoadCodes(c), 0)
  def STORE(c: String, rs2: Long, imm: Long, rs1: Long) = IMMTYPE("S", "STORE", 0, rs1, rs2, imm, StoreCodes(c), 0)
  def IMMTYPE(immtype: String, op: String = "Z", rd: Long = 0, rs1: Long = 0, rs2: Long = 0, rawimm: Long = 0, funct3: Long = 0, funct7: Long = 0) = {
    // reassemble imm number to inst
    // mask = lambda a, b=-1: (1<<a) if b == -1 else (1<<(a+1))-(1<<b)
    // bitreplace = lambda p, a, b=-1: '(imm&{})<<{}'.format(hex(mask(a,b)),p - (a if b == -1 else b)).replace('<<-','>>').replace('<<0','')
    // makeimm = lambda args: ' ^ '.join([bitreplace(*a) for a in args])
    val imm_length = Map(
      "R" -> 0,
      "I" -> 12,
      "S" -> 12,
      "U" -> 32,
      "J" -> 21,
      "B" -> 13,
      "Z" -> 0)
    val imm = rawimm & ((1L<<imm_length(immtype)) - 1)
    immtype match {
      case "R" => Opcode(op) ^ rd<<7 ^ (funct3&0x7)<<12 ^ rs1<<15 ^ rs2<<20 ^ funct7<<25 ^ (funct3&0x8)<<27
      case "I" => Opcode(op) ^ rd<<7 ^ (funct3&0x7)<<12 ^ rs1<<15 ^ (funct3&0x8)<<27 ^ imm<<20
      case "S" => Opcode(op) ^ (imm&0x1f)<<7 ^ (funct3&0x7)<<12 ^ rs1<<15 ^ rs2<<20 ^ (imm&0xfe0)<<20
      case "B" => Opcode(op) ^ (funct3&0x7)<<12 ^ rs1<<15 ^ rs2<<20 ^ (imm&0x800)>>4 ^ (imm&0x1e)<<7 ^ (imm&0x7e0)<<20 ^ (imm&0x1000)<<19
      case "U" => Opcode(op) ^ rd<<7 ^ (imm&0xfffff000)
      case "J" => Opcode(op) ^ rd<<7 ^ (imm&0xff000) ^ (imm&0x800)<<9 ^ (imm&0x7fe)<<20 ^ (imm&0x100000)<<11
      case _ => Opcode(op)
    }
  }
  def ITYPE(op: String, rd: Long, rs1: Long, imm: Long) = IMMTYPE("I", "ITYPE", rd, rs1, 0, imm, ALU(op), 0)
  def RTYPE(op: String, rd: Long, rs1: Long, rs2: Long) = IMMTYPE("R", "RTYPE", rd, rs1, rs2, 0, ALU(op), 0)
  def BRC(op: String, rs1: Long, rs2: Long, imm: Long) = IMMTYPE("B", "BRANCH", 0, rs1, rs2, imm, BranchCodes(op), 0)
  def sys = Opcode("SYSTEM")
}

object TestBinary {
  def apply(name: String, bin: scala.collection.Iterable[Long], result: Long) = new TestBinary(name, bin.to[Seq], result)
  def apply(name: String, bin: scala.collection.Iterable[Long]) = {
    import PesudoAssemblyTools._
    val newbin = bin.to[Seq] ++ Seq(
      RTYPE("XOR", 1, 0, 0))
    new TestBinary(name, newbin, 0L)
  }
}

class TestBinary(name: String, bin: Seq[Long], result: Long) {
  def name(): String = name
  def binary(): Seq[Long] = bin
  def vec() = {
    import PesudoAssemblyTools._
    val newbin = bin ++ Seq(
      STORE("SW", 1, 0, 0),
      Constants.Opcode("SYSTEM"))
    VecInit(newbin.map(_.U(32.W)))
  }
  def res() = result.U(32.W)
  def size() = bin.size + 2
}

class DatapathTester(dp: => Datapath, bin: TestBinary) extends BasicTester with TestUtils {
  val tester = Module(dp)
  val mem = Mem(1 << 24, UInt(8.W))
  val runState = RegInit(false.B)

  val writeVaild = RegInit(false.B)
  tester.io.dmem.writer.vaild := writeVaild
  val readVaild = RegInit(false.B)
  val daddr = tester.io.dmem.reader.addr
  tester.io.dmem.reader.data  := Cat(mem(daddr+3.U), mem(daddr+2.U), mem(daddr+1.U), mem(daddr))
  tester.io.dmem.reader.vaild := readVaild
  val iaddr = tester.io.imem.addr
  tester.io.imem.data  := Cat(mem(iaddr+3.U), mem(iaddr+2.U), mem(iaddr+1.U), mem(iaddr))
  tester.io.imem.vaild := runState

  val insts = bin.vec()
  val result = bin.res()
  val (cntr, done) = Counter(~runState, bin.size)

  val stopper = RegInit(0.U(16.W))
  stopper := stopper + 1.U
  when (stopper > 0x8000.U) {
    printf("time too long.\n")
    assert(false.B)
    stop(); stop()
  }

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
    when (tester.io.imem.ren & tester.io.imem.vaild) {
      printf("INS[%x] ==> %x, iaddr: %x\n", iaddr, Cat(mem(iaddr+3.U), mem(iaddr+2.U), mem(iaddr+1.U), mem(iaddr)), iaddr)
    }
    when (tester.io.dmem.writer.wen) {
      // memory write
      val waddr = tester.io.dmem.writer.addr
      val wdata = tester.io.dmem.writer.data
      switch(tester.io.dmem.writer.mode) {
        is (0.U) {
          mem(waddr) := wdata(7, 0)
          printf("MEM[%x] <== BYTE %x\n", waddr, wdata(7,0))
        }
        is (1.U) {
          mem(waddr      ) := wdata(7, 0)
          mem(waddr + 1.U) := wdata(15, 8)
          printf("MEM[%x] <== HALF %x\n", waddr, wdata(15,0))
        }
        is (2.U) {
          mem(waddr      ) := wdata(7, 0)
          mem(waddr + 1.U) := wdata(15, 8)
          mem(waddr + 2.U) := wdata(23, 16)
          mem(waddr + 3.U) := wdata(31, 24)
          printf("MEM[%x] <== WORD %x\n", waddr, wdata)
        }
        is (3.U) {
          mem(waddr      ) := wdata(7, 0)
          mem(waddr + 1.U) := wdata(15, 8)
          mem(waddr + 2.U) := wdata(23, 16)
          mem(waddr + 3.U) := wdata(31, 24)
          printf("MEM[%x] <== WORD %x\n", waddr, wdata)
        }
      }
      writeVaild := true.B
    } .otherwise {
      writeVaild := false.B
    }
    when (tester.io.dmem.reader.ren) {
      printf("MEM[%x] ==> %x\n", daddr, Cat(mem(daddr+3.U), mem(daddr+2.U), mem(daddr+1.U), mem(daddr)))
      readVaild := true.B
    } .otherwise {
      readVaild := false.B
    }
    when(tester.io.sys) {
      printf("RESULT %x ?= %x\n", Cat(mem(3), mem(2), mem(1), mem(0)), result)
      assert(Cat(mem(3), mem(2), mem(1), mem(0)) === result)
      stop(); stop()
    }
  }
}

object TestBinaryBundles {
  import PesudoAssemblyTools._

  val first = TestBinary("Hello", Seq(
    ITYPE("XOR", 2, 0, 0x2f), // r2 = 0x2f
    ITYPE("XOR", 3, 0, 0x1c), // r3 = 0x1c
    RTYPE("ADD", 2, 2, 3),    // r2 = r2 + r3
    ITYPE("XOR", 1, 2, 0x14b) // r1(ret) = r2 ^ 0x14b ; == 0x100
  ), 0x100)

  val hundredsum = TestBinary("Hundred-Sum", Seq(
    RTYPE("XOR", 2, 0, 0),      // r2(add) = 0
    RTYPE("XOR", 3, 0, 0),      // r3(sum) = 0
    ITYPE("XOR", 4, 0, 100),    // r4 = 100
    ITYPE("ADD", 2, 2, 1),      // r2 = r2 + 1 ; label
    RTYPE("ADD", 3, 3, 2),      // r3 = r3 + r2
    BRC("BLT", 2, 4, -(2L<<2)), // jump to label if r2 < r4
    RTYPE("XOR", 1, 3, 0)       // r1(ret) = r3
  ), 5050)

  val writeback = TestBinary("Write-Back", Seq(
    RTYPE("XOR", 2, 0, 0),
    ITYPE("ADD", 2, 2, 1),
    ITYPE("ADD", 2, 2, 2),
    ITYPE("ADD", 2, 2, 3),
    ITYPE("ADD", 2, 2, 4),
    RTYPE("XOR", 1, 2, 0)
  ), 10)

  val collection = Array(
    first, writeback, hundredsum
  )
}

class DatapathTests extends FlatSpec {
  TestBinaryBundles.collection foreach { test =>
    "DataPath" should s"pass ${test.name}" in {
      // Console.print(s"${test.name}:\n")
      // test.binary foreach { e =>
      //   Console.printf ("  %08x\n", e)
      // }
      assert(TesterDriver execute (() => new DatapathTester(new Datapath, test)))
    }
  }
}
