import re
import os

INPUT = "opengl32_exports.txt"
OUTPUT = "opengl32_proxy.def"

# 我们自己实现 hook 的函数名，在 .def 里不要 forward
HOOKED = {"wglSwapBuffers", "wglSwapLayerBuffers"}

def main():
    if not os.path.exists(INPUT):
        raise SystemExit(f"missing {INPUT}, 请先运行 dumpbin /exports 生成该文件")

    names = []

    with open(INPUT, "r", encoding="mbcs", errors="ignore") as f:
        for line in f:
            # dumpbin /exports 的典型行格式：
            #    123   4F 00001000 glBegin
            m = re.search(r"\s+\d+\s+[0-9A-Fa-f]+\s+[0-9A-Fa-f]+\s+(\w+)$", line)
            if m:
                name = m.group(1).strip()
                names.append(name)

    # 去重保持顺序
    seen = set()
    uniq = []
    for n in names:
        if n not in seen:
            seen.add(n)
            uniq.append(n)

    with open(OUTPUT, "w", encoding="utf-8") as out:
        out.write('LIBRARY "opengl32"\n\n')
        out.write("EXPORTS\n")

        for name in uniq:
            if name in HOOKED:
                # 由 C++ 文件实现，直接导出
                out.write(f"    {name}\n")
            else:
                # 转发到系统 opengl32.dll
                out.write(f"    {name} = opengl32.{name}\n")

    print(f"written {OUTPUT}, 共 {len(uniq)} 个导出")

if __name__ == "__main__":
    main()