// auto generate by python scripts

package rself

import chisel3._

object ALU_MODE {
  val ALU_ADD  = 0.U(4.W)
  val ALU_SUB  = 8.U(4.W)
  val ALU_SLL  = 1.U(4.W)
  val ALU_SLT  = 2.U(4.W)
  val ALU_SLTU = 3.U(4.W)
  val ALU_XOR  = 4.U(4.W)
  val ALU_SRL  = 5.U(4.W)
  val ALU_SRA  = 13.U(4.W)
  val ALU_OR   = 6.U(4.W)
  val ALU_AND  = 7.U(4.W)
}

object IMM_MODE {
  val IMM_R = 0.U(3.W)
  val IMM_I = 1.U(3.W)
  val IMM_S = 2.U(3.W)
  val IMM_U = 3.U(3.W)
  val IMM_J = 4.U(3.W)
  val IMM_B = 5.U(3.W)
  val IMM_Z = 6.U(3.W)
}

object SL_TYPE {
  val SL_B = 0.U(2.W)
  val SL_H = 1.U(2.W)
  val SL_W = 2.U(2.W)
  val SL_D = 3.U(2.W)
}

object BR_TYPE {
  val BR_EQ  = 0.U(3.W)
  val BR_NE  = 1.U(3.W)
  val BR_LT  = 4.U(3.W)
  val BR_GE  = 5.U(3.W)
  val BR_LTU = 6.U(3.W)
  val BR_GEU = 7.U(3.W)
}

