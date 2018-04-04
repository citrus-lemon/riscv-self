package rself

import chisel3._

trait TestUtils {
  val rnd = new scala.util.Random(System.currentTimeMillis)

  type TestList = scala.collection.mutable.ListBuffer[Long]
  object TestList {
    import scala.collection.mutable.ListBuffer
    def apply(): ListBuffer[Long] = {
      val ref: ListBuffer[Long] = ListBuffer()
      ref
    }
    def List4() = (TestList(), TestList(), TestList(), TestList())
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

  def interface(l: scala.collection.Iterable[Long], bits: Int = 32) = {
    VecInit(l.to[Seq].map(_.U(bits.W)))
  }

  type List4 = Tuple4[ListBuffer[Long], ListBuffer[Long], ListBuffer[Long], ListBuffer[Long]]

  def randomInsts(r: Int = 5): List4 = {
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

  def immData(inst: Long, mode: String): Long = {
    def B(hi: Int, lo: Int = -1) = {
      val low = if (lo == -1) {hi} else {lo}
      (inst & ((1<<(hi+1)) - (1<<low))) >> low
    }
    return (mode match {
      // operator `<<' higher than `^'
      case "R" => 0
      case "I" => B(31, 20)
      case "S" => B(31, 25)<<5 ^ B(11, 7)
      case "B" => B(31)<<12 ^ B(7)<<11 ^ B(30, 25)<<5 ^ B(11, 8)<<1
      case "J" => B(31)<<20 ^ B(19, 12)<<12 ^ B(20)<<11 ^ B(30, 21)<<1
      case "U" => B(31, 12)<<12
      case "Z" => 0
      case  _  => 0
    })
  }

  def calculate(a: Long, b: Long, mod: Long): Long = {
    val shamt = b % (1<<5)
    val opstr = Constants.ALU.toMap.map(_.swap).get(mod).get
    return (opstr match {
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
    }).toUnsigned()
  }

  def branchCond(fn: String, rrs: Tuple2[Long, Long]): Long = (if (fn match {
    case "BEQ" => rrs._1 == rrs._2
    case "BNE" => rrs._1 != rrs._2
    case "BLT" => rrs._1.toInt < rrs._2.toInt
    case "BGE" => rrs._1.toInt >= rrs._2.toInt
    case "BLTU" => rrs._1 < rrs._2
    case "BGEU" => rrs._1 >= rrs._2
  }) {1L} else {0L})
}
