package rself

import chisel3._
import chisel3.util._
import chisel3.testers._
import org.scalatest.FlatSpec

class ALUTester(alu: => ALU) extends BasicTester with TestUtils {
  val tester = Module(alu)
  val alu_A   = TestList()
  val alu_B   = TestList()
  val alu_op  = TestList()
  val alu_out = TestList()
  for (op <- Constants.ALU; i <- 0 until 10) {
    val (opstr, opcode) = op
    val (a, b) = (random32(), random32())
    val shamt = b % (1<<5)
    alu_op += opcode; alu_A += a; alu_B += b
    alu_out += ((opstr match {
      case "ADD" => a + b
      case "SUB" => a - b
      case "XOR" => a ^ b
      case "AND" => a & b
      case "OR"  => a | b
      case "SLT" => if (a.toInt < b.toInt) {1L} else {0L}
      case "SLTU"=> if (a < b) {1L} else {0L}
      case "SLL" => a << shamt
      case "SRL" => a >> shamt
      case "SRA" => (a.toInt >> shamt).toLong
      case _ => 0xffffffffL
    }) & ((1L<<32) - 1))
  }
  alu_A += 0L; alu_B += 0xfece1234L; alu_op += 15; alu_out += 0xfece1234L // test invaild operator
  shuffle(alu_A, alu_B, alu_op, alu_out)
  alu_A += 0L; alu_B += 0xfece1234L; alu_op += 15; alu_out += 0xfece1234L // can not reach last data

  val a = interface(alu_A)
  val b = interface(alu_B)
  val p = interface(alu_op, 4)
  val o = interface(alu_out)
  val (cntr, done) = Counter(true.B, o.size)
  tester.io.A := a(cntr)
  tester.io.B := b(cntr)
  tester.io.alu_op := p(cntr)

  when(done) { stop(); stop() }
  printf("Counter: %d, Op: %d, A: 0x%x, B: 0x%x, Out: %x ?= %x\n",
    cntr, tester.io.alu_op, tester.io.A, tester.io.B, tester.io.out, o(cntr))
  assert(tester.io.out === o(cntr))
}

class ALUTests extends FlatSpec {
  "ALU" should "pass" in {
    assert(TesterDriver execute (() => new ALUTester(new ALU)))
  }
}