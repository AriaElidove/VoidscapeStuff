// noinspection ES6ConvertVarToLetConst

var ASM = Java.type('net.neoforged.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

// noinspection JSUnusedGlobalSymbols
function initializeCoreMod() {
    ASM.loadFile('META-INF/asm/util/util.js');
    return {
        'visibility': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.renderer.LightTexture',
                'methodName': 'getBrightness',
                'methodDesc': '(Lnet/minecraft/world/level/dimension/DimensionType;I)F'
            },
            'transformer': function (/*org.objectweb.asm.tree.MethodNode*/ methodNode) {
                var /*org.objectweb.asm.tree.InsnList*/ instructions = methodNode.instructions;
                instructions.insertBefore(
                    ASM.findFirstInstruction(methodNode, Opcodes.FRETURN),
                    ASM.listOf(
                        new VarInsnNode(Opcodes.ILOAD, 1),
                        new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            'tamaized/voidscape/asm/ASMHooks',
                            'visibility',
                            '(FI)F',
                            false
                            )
                        )
                    );
                return methodNode;
            }
        },
        'nonightvision': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.renderer.LightTexture',
                'methodName': 'updateLightTexture',
                'methodDesc': '(F)V'
            },
            'transformer': function (/*org.objectweb.asm.tree.MethodNode*/ methodNode) {
                var /*org.objectweb.asm.tree.InsnList*/ instructions = methodNode.instructions;
                instructions.insert(
                    findLastVarInstruction(methodNode, Opcodes.FLOAD, 9),
                    ASM.listOf(
                        new VarInsnNode(Opcodes.ALOAD, 2),
                        new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            'tamaized/voidscape/asm/ASMHooks',
                            'cancelNightVision',
                            '(FLnet/minecraft/world/level/Level;)F',
                            false
                            )
                        )
                    );
                return methodNode;
            }
        },
        'nogamma': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.renderer.LightTexture',
                'methodName': 'updateLightTexture',
                'methodDesc': '(F)V'
            },
            'transformer': function (/*org.objectweb.asm.tree.MethodNode*/ methodNode) {
                var /*org.objectweb.asm.tree.InsnList*/ instructions = methodNode.instructions;
                instructions.insert(
                    ASM.findFirstMethodCall(methodNode,
                        ASM.MethodType.VIRTUAL,
                        'net/minecraft/client/Options',
                        'gamma',
                        '()Lnet/minecraft/client/OptionInstance;'
                        ).getNext().getNext().getNext(),
                    ASM.listOf(
                        new VarInsnNode(Opcodes.ALOAD, 2),
                        new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            'tamaized/voidscape/asm/ASMHooks',
                            'cancelGamma',
                            '(FLnet/minecraft/world/level/Level;)F',
                            false
                            )
                        )
                    );
                return methodNode;
            }
        }
    }
}
