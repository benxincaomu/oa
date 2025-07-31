

export default async function WorkflowList({
  params,
}: {
  params: { workbenchId: string };
}) { 
    const {workbenchId} = await params;

    return (
        <div>
            {workbenchId}
        </div>
    )
}