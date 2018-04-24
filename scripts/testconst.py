from utility import *

filename = 'src/test/scala/Constants.scala'
writer = ScalaWriter(filename)
namespace = writer.namespace

B = getbits

@ScalaWriter.Seq('ALU')
def ALUmode():
    return [('"%s"' % code['name'].upper(), '{}L', B(code, 14, 12) + (B(code, 30) << 3)) for code in codes if B(code, 6, 2) == 0b01100]

@ScalaWriter.Seq('Opcode')
def Opcode():
    fmt = '{}L'
    return map(lambda x:('"' + x[0] + '"', fmt, x[1]), [
        ('LUI',    0b0110111),
        ('AUIPC',  0b0010111),
        ('JAL',    0b1101111),
        ('JALR',   0b1100111),
        ('BRANCH', 0b1100011),
        ('LOAD',   0b0000011),
        ('STORE',  0b0100011),
        ('ITYPE',  0b0010011),
        ('RTYPE',  0b0110011),
        ('MEMORY', 0b0001111),
        ('SYSTEM', 0b1110011),
    ])

@ScalaWriter.Seq('BranchCodes')
def BranchCodes():
    return [('"%s"' % code['name'].upper(), '{}L', B(code, 14, 12)) for code in codes if B(code, 6, 2) == 0b11000]

@ScalaWriter.Seq('LoadCodes')
def LoadCodes():
    return [('"%s"' % code['name'].upper(), '{}L', B(code, 14, 12)) for code in codes if B(code, 6, 2) == 0b00000]

@ScalaWriter.Seq('StoreCodes')
def StoreCodes():
    return [('"%s"' % code['name'].upper(), '{}L', B(code, 14, 12)) for code in codes if B(code, 6, 2) == 0b01000]

@ScalaWriter.Seq('ControlActs')
def ControlActs():
    c = ["ALU", "BRC", "LDS", "FEN", "SYS", "LUI", "AUI", "JMP"]
    fmt = '{}L'
    return [('"' + c[i] + '"', fmt, i) for i in range(len(c))]

@ScalaWriter.Seq('ImmModes')
def ImmModes():
    modes = 'RISUJBZ'
    fmt = '{}L'
    return [('"' + modes[i] + '"', fmt, i) for i in range(len(modes))]

@namespace('Constants')
def constant():
    return [
        *ALUmode(),
        *Opcode(),
        *BranchCodes(),
        *LoadCodes(),
        *StoreCodes(),
        *ControlActs(),
        *ImmModes(),
    ]
