from utility import *

filename = 'src/test/scala/Constants.scala'
writer = ScalaWriter(filename)
namespace = writer.namespace

B = getbits

@ScalaWriter.Seq('ALU')
def ALUmode():
  return [('"%s"' % code['name'].upper(), '{}', B(code, 14, 12) + (B(code, 30) << 3)) for code in codes if B(code, 6, 2) == 0b01100]

@namespace('Constants')
def constant():
  return ALUmode()