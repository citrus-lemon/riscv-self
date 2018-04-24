#!/usr/bin/env python3
'parse opcodes from risc-v opcodes'

import os
import io

def fileparse(fs, default_class=''):
    comment = default_class
    if isinstance(fs, str):
        fs = fs.split("\n")
    elif isinstance(fs, io.IOBase):
        comment = default_class or os.path.basename(fs.name)
        fs = fs.readlines()
    codelist = []
    for line in [x.strip() for x in fs]:
        if line == '':
            continue
        elif line[0] == '#':
            comment = line[1:].strip()
        else:
            i = line.find('#')
            linecomment = ''
            if i != -1:
                linecomment = line[i+1:].strip()
                line = line[:i].strip()
            codelist.append((line, comment, linecomment))
    return codelist

class UndefinedParameterError(NotImplementedError):
    'code type undefined'

class InvaildInstructionError(ValueError):
    'code invaild length'

DefinedParameter = {
    'rd': (11, 7),
    'rs1': (19, 15),
    'rs2': (24, 20),
    'imm12': (31, 20),
    'imm12hi': (31, 25),
    'imm12lo': (11, 7),
    'imm20': (31, 12),
    'jimm20': (31, 12),
    'bimm12hi': (31, 25),
    'bimm12lo': (11, 7),
    'shamt': (25, 20),
    'shamtw': (24, 20),
    'pred': (27, 24),
    'succ': (23, 20),
}

def T(s):
    a = list(s)
    a.sort()
    return tuple(a)

InstructionType = {
    T({'rd', 'imm20'}): 'U',
    T({'rd', 'jimm20'}): 'J',
    T({'rd', 'rs1', 'imm12'}): 'I',
    T({'rd', 'rs1', 'shamt'}): 'I',
    T({'rd', 'rs1', 'rs2'}): 'R',
    T({'rs1', 'rs2', 'bimm12hi', 'bimm12lo'}): 'B',
    T({'rs1', 'rs2', 'imm12hi', 'imm12lo'}): 'S',
}

def codeparse(code):
    if isinstance(code, tuple):
        code = code[0]
    sig, *args = code.split()
    args_list = []
    arg_set = set()
    for arg in args:
        pos, *val = arg.split('=')
        if not val:
            if not pos in DefinedParameter:
                raise UndefinedParameterError(pos)
            arg_set.add(pos)
            args_list.append((pos, DefinedParameter[pos], None))
        else:
            hi, *loa = pos.split('..')
            r = (int(hi), int(loa[0])) if loa else (int(hi), int(hi))
            if val[0] == 'ignore':
                args_list.append(('var', r, None))
            else:
                if val[0].startswith('0x'):
                    valnum = int(val[0], 16)
                elif val[0].startswith('0b'):
                    valnum = int(val[0], 2)
                else:
                    valnum = int(val[0])
                args_list.append(('const', r, valnum))

    # Check Instruction Type
    index = T(arg_set)
    if index in InstructionType:
        codetype = InstructionType[index]
    else:
        codetype = 'O'

    # Check Instruction Vaild
    mask = 0
    constmask = 0
    const = 0
    for part in args_list:
        mask ^= (lambda x:(1<<(x[0]+1)) - (1<<x[1]))(part[1])
        if part[0] == 'const':
            constmask ^= (lambda x:(1<<(x[0]+1)) - (1<<x[1]))(part[1])
            const ^= (lambda x, y:y<<x[1])(part[1], part[2])

    length = None
    for bit_length in range(4,8):
        if mask ^ ((1<<(1<<bit_length))-1) == 0:
            length = (1<<bit_length)
            break
    if not length:
        raise InvaildInstructionError(sig, bin(mask))

    return {
        'name': sig,
        'args': args_list,
        'type': codetype,
        'len': length,
        'const': const,
        'mask': constmask,
    }

filename = 'opcodes/opcodes'

with open(filename) as fp:
    content = fp.readlines()
    content.insert(9, '# RV32I')
    content = fileparse(content)

codes = [codeparse(code) for code in filter(lambda x:x[1] == 'RV32I' or x[1] == 'SYSTEM', content)]

if __name__ == '__main__':
    import pprint
    pp = pprint.PrettyPrinter(indent=2).pprint
    pp(codes)
