package mod.patrigan.structure_toolkit.world.gen.processors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.patrigan.structure_toolkit.util.RandomType;
import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.gen.feature.template.IStructureProcessorType;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.StructureProcessor;
import net.minecraft.world.gen.feature.template.Template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static mod.patrigan.structure_toolkit.init.ModProcessors.CEILING_ATTACHMENT;
import static mod.patrigan.structure_toolkit.util.RandomType.RANDOM_TYPE_CODEC;
import static mod.patrigan.structure_toolkit.world.gen.processors.ProcessorUtil.getBlock;
import static mod.patrigan.structure_toolkit.world.gen.processors.ProcessorUtil.isSolid;
import static net.minecraft.block.Blocks.AIR;
import static net.minecraftforge.registries.ForgeRegistries.BLOCKS;

public class CeilingAttachmentProcessor extends StructureProcessor {
    public static final Codec<CeilingAttachmentProcessor> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    ResourceLocation.CODEC.fieldOf("block").forGetter(data -> data.block),
                    Codec.BOOL.optionalFieldOf("needs_wall", false).forGetter(processor -> processor.needsWall),
                    Codec.FLOAT.fieldOf("rarity").forGetter(processor -> processor.rarity),
                    RANDOM_TYPE_CODEC.optionalFieldOf("random_type", RandomType.BLOCK).forGetter(processor -> processor.randomType)
            ).apply(builder, CeilingAttachmentProcessor::new));
    private static final long SEED = 7645816L;

    private final ResourceLocation block;
    private final boolean needsWall;
    private final float rarity;
    private final RandomType randomType;

    public CeilingAttachmentProcessor(ResourceLocation block, boolean needsWall, float rarity, RandomType randomType) {
        this.block = block;
        this.needsWall = needsWall;
        this.rarity = rarity;
        this.randomType = randomType;
    }

    @Override
    public Template.BlockInfo process(IWorldReader world, BlockPos piecePos, BlockPos structurePos, Template.BlockInfo rawBlockInfo, Template.BlockInfo blockInfo, PlacementSettings settings, Template template) {
        Random random = ProcessorUtil.getRandom(randomType, blockInfo.pos, piecePos, structurePos, world, SEED);
        BlockState blockstate = blockInfo.state;
        BlockPos blockpos = blockInfo.pos;
        if(blockstate.getBlock().equals(AIR) && random.nextFloat() <= rarity){
            List<Template.BlockInfo> pieceBlocks = settings.getRandomPalette(template.palettes, piecePos).blocks();
            if(hasCeiling(pieceBlocks, rawBlockInfo.pos)) {
                if(needsWall) {
                    if(hasWall(pieceBlocks, rawBlockInfo.pos)){
                        return new Template.BlockInfo(blockpos, BLOCKS.getValue(block).defaultBlockState(), blockInfo.nbt);
                    }
                } else {
                    return new Template.BlockInfo(blockpos, BLOCKS.getValue(block).defaultBlockState(), blockInfo.nbt);
                }
            }
        }
        return blockInfo;
    }

    private boolean hasWall(List<Template.BlockInfo> pieceBlocks, BlockPos pos) {
        List<Template.BlockInfo> neighbours = getSideNeighboursBlockInfo(pieceBlocks, pos, true);
        long count = neighbours.stream().filter(ProcessorUtil::isSolid).count();
        return count > 0;
    }

    private List<Template.BlockInfo> getSideNeighboursBlockInfo(List<Template.BlockInfo> pieceBlocks, BlockPos pos, boolean diagonal) {
        List<Template.BlockInfo> neighbours = new ArrayList<>(Arrays.asList(getBlock(pieceBlocks, pos.north()), getBlock(pieceBlocks, pos.south()), getBlock(pieceBlocks, pos.west()), getBlock(pieceBlocks, pos.east())));
        if(diagonal){
            neighbours.addAll(Arrays.asList(
                    getBlock(pieceBlocks, pos.north().east()),
                    getBlock(pieceBlocks, pos.east().south()),
                    getBlock(pieceBlocks, pos.south().west()),
                    getBlock(pieceBlocks, pos.west().north())
            ));
        }
        return neighbours;
    }

    private boolean hasCeiling(List<Template.BlockInfo> pieceBlocks, BlockPos pos) {
        return isSolid(getBlock(pieceBlocks, pos.above()));
    }

    protected IStructureProcessorType<?> getType() {
        return CEILING_ATTACHMENT;
    }
}