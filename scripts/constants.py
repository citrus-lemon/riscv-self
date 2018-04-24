from utility import *

filename = 'src/main/scala/Constants.scala'
namespace = ScalaWriter(filename).constants

B = getbits

@namespace('ALU_MODE')
def ALUmode():
    return [('ALU_'+code['name'].upper(), '{}.U(4.W)', B(code, 14, 12) + (B(code, 30) << 3)) for code in codes if B(code, 6, 2) == 0b01100]

@namespace('IMM_MODE')
def immMode():
    fmt = '{}.U(3.W)'
    modes = 'RISUJBZ'
    return [('IMM_'+modes[i], fmt, i) for i in range(len(modes))]

@namespace('SL_TYPE')
def saveloadtype():
    return [('SL_'+code['name'].upper()[1:], '{}.U(2.W)', B(code, 13, 12)) for code in codes if B(code, 6, 2) == 0b01000]

@namespace('BR_TYPE')
def brtype():
    return [('BR_'+code['name'].upper()[1:], '{}.U(3.W)', B(code, 14, 12)) for code in codes if B(code, 6, 2) == 0b11000]
