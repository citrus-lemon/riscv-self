package rself

import chisel3._

trait TestUtils {
  val rnd = new scala.util.Random(System.currentTimeMillis)

  object TestList {
    import scala.collection.mutable.ListBuffer
    def apply(): ListBuffer[Long] = {
      val ref: ListBuffer[Long] = ListBuffer()
      ref
    }
  }
  import scala.collection.mutable.ListBuffer

  def randombits(bl: Int) = {rnd.nextLong() & ((1L<<bl)-1)}
  def random32() = randombits(32)

  implicit class SignedLong(me: Long) {
    def toSigned(b: Int = 32):Long = {val rme = me & ((1L<<b) - 1);if ((me & (1L << (b-1))) == 0) {rme} else {rme - (1L<<b)}}
    def toUnsigned(b: Int = 32):Long = me & ((1L<<b) - 1)
  }

  def shuffle[T, CC[X] <: scala.collection.mutable.AbstractBuffer[X]](xs: CC[T]*): Unit = {
    val size = xs(0).size
    val index = scala.util.Random.shuffle((0 until size).toList)
    for (el <- xs) {
      val clone = el.clone
      for (i <- 0 until size) {
        el(i) = clone(index(i))
      }
    }
  }

  def interface(l: ListBuffer[Long], bits: Int = 32) = {
    VecInit(l.to[Seq].map(_.U(bits.W)))
  }

  def randomInsts(r: Int = 5): Tuple4[ListBuffer[Long], ListBuffer[Long], ListBuffer[Long], ListBuffer[Long]] = {
    val ctl_inst = TestList()
    val ctl_mode = TestList()
    val ctl_sel  = TestList()
    val ctl_imm  = TestList()
    import chisel3.core.UInt
    def append(t:Long, m:Long, s:Long, i:Long): Unit = {
      ctl_inst += t; ctl_mode += m; ctl_sel += s; ctl_imm += i
    }
    def appendEle(t:Long, m:Long, s:UInt, i:UInt): Unit = append(t, m.toLong, s.litValue().toLong, i.litValue().toLong)
    // operator `<<' higher than `^'
    for (op <- Constants.Opcode; i <- 0 until r) {
      val (opstr, opcode) = op
      import IMM_MODE._
      opstr match {
        case "LUI" =>
          appendEle((opcode ^ randombits(5)<<7 ^ randombits(20)<<12), 0, ACT.LUI, IMM_U)
        case "AUIPC" =>
          appendEle((opcode ^ randombits(5)<<7 ^ randombits(20)<<12), 0, ACT.AUI, IMM_U)
        case "JAL" =>
          appendEle((opcode ^ randombits(5)<<7 ^ randombits(20)<<12), 1, ACT.JMP, IMM_J)
        case "JALR" =>
          appendEle((opcode ^ randombits(5)<<7 ^ randombits(5)<<15 ^ randombits(12)<<20),
            0, ACT.JMP, IMM_I)
        case "BRANCH" => {
          for (bfunc <- Constants.BranchCodes) {
            val (bfs, bfmode) = bfunc
            appendEle((opcode ^ randombits(5)<<7 ^ bfmode<<12 ^ randombits(5)<<15 ^ randombits(5)<<20 ^ randombits(7)<<25),
              bfmode, ACT.BRC, IMM_B)
          }
        }
        case "LOAD" => {
          for (lfunc <- Constants.LoadCodes) {
            val (lfs, lfmode) = lfunc
            appendEle((opcode ^ randombits(5)<<7 ^ lfmode<<12 ^ randombits(5)<<15 ^ randombits(12)<<20),
              lfmode, ACT.LDS, IMM_I)
          }
        }
        case "STORE" => {
          for (sfunc <- Constants.LoadCodes) {
            val (sfs, sfmode) = sfunc
            appendEle((opcode ^ randombits(5)<<7 ^ sfmode<<12 ^ randombits(5)<<15 ^ randombits(5)<<20 ^ randombits(7)<<25),
              8 ^ sfmode, ACT.LDS, IMM_S)
          }
        }
        case "ITYPE" => {
          for (alufunc <- Constants.ALU) {
            var (alstr, alumode) = alufunc
            if (alstr == "SUB") {
              alstr = "ADD"
              alumode = alumode & 7
            }
            val alcode = alumode & 7
            val hicode = alumode >> 3
            val imm = if ((alcode & 3) == 1) {
              hicode<<10 ^ randombits(5)
            } else {
              randombits(12)
            }
            appendEle((opcode ^ randombits(5)<<7 ^ alcode<<12 ^ randombits(5)<<15 ^ imm<<20),
              alumode, ACT.ALU, IMM_I)
          }
        }
        case "RTYPE" => {
          for (alufunc <- Constants.ALU) {
            val (alstr, alumode) = alufunc
            val alcode = alumode & 7
            val hicode = alumode >> 3
            appendEle((opcode ^ randombits(5)<<7 ^ alcode<<12 ^ randombits(5)<<15 ^ randombits(5)<<20 ^ hicode<<30),
              alumode, ACT.ALU, IMM_R)
          }
        }
        case "MEMORY" => {
          for (i <- 0 until 2) {
            appendEle((opcode ^ i<<12), 0, ACT.FEN, IMM_Z)
          }
        }
        case "SYSTEM" => {
          for (i <- 0 until 2) {
            appendEle((opcode ^ i<<20), 0, ACT.SYS, IMM_Z)
          }
        }
        case _ =>
      }
    }
    return (ctl_inst, ctl_mode, ctl_sel, ctl_imm)
  }
}
